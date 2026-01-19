"""
이미지 다운로드 및 리사이징 스크립트 (로컬 저장용)
NCP Object Storage에서 이미지를 다운로드하여 리사이징 후 로컬에 저장
"""

import os
import urllib.parse
import mysql.connector
import requests
from io import BytesIO
from PIL import Image

# ========== 설정 ==========
def required_env(name):
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(f"Missing required env var: {name}")
    return value

DB_CONFIG = {
    'host': os.environ.get('DB_HOST', '127.0.0.1'),
    'port': int(os.environ.get('DB_PORT', 13306)),
    'user': required_env('DB_USER'),
    'password': required_env('DB_PASSWORD'),
    'database': os.environ.get('DB_NAME', 'guesthouse')
}

# 로컬 저장 폴더
OUTPUT_DIR = 'resized_images'
MAX_WIDTH = 1600
MAX_HEIGHT = 1600

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

def get_extension(url):
    """URL에서 확장자 추출"""
    lower_url = url.lower()
    if '.png' in lower_url:
        return 'png'
    elif '.gif' in lower_url:
        return 'gif'
    elif '.webp' in lower_url:
        return 'webp'
    return 'jpg'

def resize_image(image_data, extension):
    """이미지 리사이징"""
    if extension not in ('jpg', 'png', 'webp'):
        return image_data

    try:
        with Image.open(BytesIO(image_data)) as img:
            width, height = img.size
            if width <= MAX_WIDTH and height <= MAX_HEIGHT:
                return image_data

            # JPEG는 RGB로 변환
            if extension == 'jpg' and img.mode not in ('RGB', 'L'):
                img = img.convert('RGB')
            elif extension in ('png', 'webp') and img.mode == 'P':
                img = img.convert('RGBA')

            img.thumbnail((MAX_WIDTH, MAX_HEIGHT), Image.Resampling.LANCZOS)
            buffer = BytesIO()
            save_format = 'JPEG' if extension == 'jpg' else extension.upper()
            save_kwargs = {'optimize': True}
            if save_format == 'JPEG':
                save_kwargs['quality'] = 95

            img.save(buffer, format=save_format, **save_kwargs)
            return buffer.getvalue()
    except Exception as e:
        print(f"  리사이징 실패: {e}")
        return image_data

def process_table(cursor, table_name, id_column, url_column, folder):
    """테이블 처리"""
    print(f"\n{'='*50}")
    print(f"테이블: {table_name} → 폴더: {folder}")
    print(f"{'='*50}")
    
    # NCP 이미지 조회
    cursor.execute(f"""
        SELECT {id_column}, {url_column} 
        FROM {table_name} 
        WHERE {url_column} LIKE '%kr.object.ncloudstorage.com%'
    """)
    
    rows = cursor.fetchall()
    total = len(rows)
    print(f"처리 대상: {total}개\n")
    
    # 로컬 폴더 생성
    local_folder = os.path.join(OUTPUT_DIR, folder)
    os.makedirs(local_folder, exist_ok=True)
    
    success_count = 0
    fail_count = 0
    
    for i, (row_id, url) in enumerate(rows, 1):
        print(f"[{i}/{total}] ID={row_id}")
        
        if not url:
            print("  URL 없음")
            continue
        
        # 다운로드
        image_data = download_image(url)
        if not image_data:
            print("  다운로드 실패")
            fail_count += 1
            continue
        
        # 확장자
        ext = get_extension(url)
        
        # 리사이징
        resized_data = resize_image(image_data, ext)
        
        # 로컬 저장 (원본 파일명 유지)
        filename = os.path.basename(url.split('?')[0])  # 쿼리 제거
        if not filename.endswith(f'.{ext}'):
            filename = f"{row_id}.{ext}"
        
        filepath = os.path.join(local_folder, filename)
        with open(filepath, 'wb') as f:
            f.write(resized_data)
        
        print(f"  ✓ 저장: {filepath}")
        success_count += 1
    
    print(f"\n{table_name} 완료: 성공 {success_count}, 실패 {fail_count}")
    return success_count, fail_count

def main():
    print("="*60)
    print("이미지 다운로드 및 리사이징 (로컬 저장)")
    print("="*60)
    
    # DB 연결
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    total_success = 0
    total_fail = 0
    
    try:
        # 1. accommodation_image
        s, f = process_table(
            cursor,
            table_name='accommodation_image',
            id_column='image_id',
            url_column='image_url',
            folder='resizing_accommodation'
        )
        total_success += s
        total_fail += f
        
        # 2. room
        s, f = process_table(
            cursor,
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
    print(f"저장 폴더: {os.path.abspath(OUTPUT_DIR)}")
    print("="*60)
    print("\n다음 단계:")
    print("1. Cyberduck으로 NCP Object Storage 연결")
    print("2. resized_images/resizing_accommodation → NCP/resizing_accommodation 업로드")
    print("3. resized_images/resizing_room → NCP/resizing_room 업로드")
    print("4. DB에서 image_url 업데이트 (별도 스크립트 필요)")

if __name__ == '__main__':
    main()
