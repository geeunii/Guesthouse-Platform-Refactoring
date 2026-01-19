"""
이미지 마이그레이션 스크립트
네이버 CDN URL에서 원본 이미지를 다운로드하여 Naver Object Storage에 업로드

대상 테이블:
1. accommodation_image (image_url)
2. room (main_image_url)
"""

import mysql.connector
import requests
import urllib.parse
import uuid
import time
from io import BytesIO
from PIL import Image, ImageFilter
import os

# ========== 설정 ==========
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 13306,
    'user': 'thismo',
    'password': 'thismo1234',
    'database': 'guesthouse'
}

# 이미지 리사이징 옵션 (선명도를 위해 해상도 상한을 넉넉하게 잡음)
MAX_WIDTH = 2600
MAX_HEIGHT = 2600
RESIZE_TARGET_FOLDERS = {'accommodation_image', 'room', 'resizing_accommodation', 'resizing_room'}
LOCAL_SAVE_DIR = 'resized_images'

def extract_original_url(naver_cdn_url):
    """네이버 CDN URL에서 원본 이미지 URL 추출"""
    if 'src=' not in naver_cdn_url:
        return naver_cdn_url
    
    try:
        # src= 파라미터 추출
        src_part = naver_cdn_url.split('src=')[1]
        # URL 디코딩
        original_url = urllib.parse.unquote(src_part)
        return original_url
    except:
        return naver_cdn_url

def download_image(url):
    """이미지 다운로드"""
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        response = requests.get(url, headers=headers, timeout=30)
        if response.status_code == 200:
            return response.content
    except Exception as e:
        print(f"  다운로드 실패: {e}")
    return None

def get_extension(url, content_type=None):
    """URL 또는 Content-Type에서 확장자 추출"""
    if content_type:
        if 'png' in content_type:
            return 'png'
        elif 'gif' in content_type:
            return 'gif'
        elif 'webp' in content_type:
            return 'webp'
    
    # URL에서 추출
    lower_url = url.lower()
    if '.png' in lower_url:
        return 'png'
    elif '.gif' in lower_url:
        return 'gif'
    elif '.webp' in lower_url:
        return 'webp'
    
    return 'jpg'

def resize_image_if_needed(image_data, extension):
    """Pillow로 크기를 제한(최대 해상도 유지)하고 샤프닝을 적용한다."""
    if extension not in ('jpg', 'png', 'webp'):
        return image_data

    try:
        with Image.open(BytesIO(image_data)) as img:
            width, height = img.size
            if width <= MAX_WIDTH and height <= MAX_HEIGHT:
                return image_data

            ratio = min(MAX_WIDTH / width, MAX_HEIGHT / height)
            new_size = (int(width * ratio), int(height * ratio))

            # JPEG 저장 시에는 RGB로 변환
            if extension == 'jpg' and img.mode not in ('RGB', 'L'):
                img = img.convert('RGB')
            elif extension in ('png', 'webp') and img.mode == 'P':
                img = img.convert('RGBA')

            img = img.resize(new_size, Image.Resampling.LANCZOS)
            img = img.filter(ImageFilter.SHARPEN)
            buffer = BytesIO()
            save_format = 'JPEG' if extension == 'jpg' else extension.upper()
            save_kwargs = {'optimize': True}
            if save_format == 'JPEG':
                save_kwargs['quality'] = 98  # 선명도 우선

            img.save(buffer, format=save_format, **save_kwargs)
            return buffer.getvalue()
    except Exception as e:
        print(f"  리사이징 실패 (원본 유지): {e}")
        return image_data

def save_locally(image_data, folder, filename):
    """권한 없이 로컬에만 저장"""
    output_dir = os.path.join(LOCAL_SAVE_DIR, folder)
    os.makedirs(output_dir, exist_ok=True)
    path = os.path.join(output_dir, filename)
    with open(path, 'wb') as f:
        f.write(image_data)
    return path

def migrate_table(cursor, conn, table_name, id_column, url_column, folder):
    """테이블의 이미지 마이그레이션"""
    print(f"\n{'='*50}")
    print(f"테이블: {table_name}")
    print(f"{'='*50}")
    
    # NCP에 이미 있는 이미지를 재처리하여 로컬로 저장
    cursor.execute(f"""
        SELECT {id_column}, {url_column} 
        FROM {table_name} 
        WHERE {url_column} LIKE '%kr.object.ncloudstorage.com%'
    """)
    
    rows = cursor.fetchall()
    total = len(rows)
    print(f"마이그레이션 대상: {total}개\n")
    
    success_count = 0
    fail_count = 0
    
    for i, (row_id, old_url) in enumerate(rows, 1):
        print(f"[{i}/{total}] ID={row_id}")
        
        if not old_url:
            print("  URL 없음, 스킵")
            continue
        
        # 원본 URL 추출
        original_url = extract_original_url(old_url)
        print(f"  원본 URL: {original_url[:80]}...")
        
        # 이미지 다운로드
        image_data = download_image(original_url)
        if not image_data:
            # 원본 URL 실패 시 CDN URL로 시도
            image_data = download_image(old_url)
            if not image_data:
                print("  다운로드 실패")
                fail_count += 1
                continue
        
        # 확장자 결정
        ext = get_extension(original_url)

        # accommodation_image/room 폴더는 업로드 전에 리사이징
        if folder in RESIZE_TARGET_FOLDERS:
            image_data = resize_image_if_needed(image_data, ext)
        
        parsed = urllib.parse.urlparse(original_url)
        basename = os.path.basename(parsed.path)
        if not basename:
            basename = f"{uuid.uuid4()}.{ext}"
        saved_path = save_locally(image_data, folder, basename)
        
        print(f"  ✓ 저장 완료: {saved_path}")
        success_count += 1
        
        # Rate limiting
        time.sleep(0.1)
    
    print(f"\n{table_name} 완료: 성공 {success_count}, 실패 {fail_count}")
    return success_count, fail_count

def main():
    print("="*60)
    print("이미지 마이그레이션 시작")
    print("네이버 CDN -> Naver Object Storage")
    print("="*60)
    
    # DB 연결
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    total_success = 0
    total_fail = 0
    
    try:
        # 1. accommodation_image 테이블
        s, f = migrate_table(
            cursor, conn,
            table_name='accommodation_image',
            id_column='image_id',
            url_column='image_url',
            folder='resizing_accommodation'
        )
        total_success += s
        total_fail += f
        
        # 2. room 테이블
        s, f = migrate_table(
            cursor, conn,
            table_name='room',
            id_column='room_id',
            url_column='main_image_url',
            folder='resizing_room'
        )
        total_success += s
        total_fail += f
        
    finally:
        cursor.close()
        conn.close()
    
    print("\n" + "="*60)
    print(f"전체 완료: 성공 {total_success}, 실패 {total_fail}")
    print("="*60)

if __name__ == '__main__':
    main()
