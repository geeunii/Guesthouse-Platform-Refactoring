"""
이미지 다운로드 스크립트 (1단계)
네이버 CDN URL에서 원본 이미지를 다운로드하여 로컬에 저장

Cyberduck으로 업로드 후 2단계 스크립트로 DB 업데이트
"""

import mysql.connector
import requests
import urllib.parse
import os
import json
import time

# ========== 설정 ==========
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 13306,
    'user': 'thismo',
    'password': 'thismo1234',
    'database': 'guesthouse'
}

# 다운로드 폴더
DOWNLOAD_DIR = 'd:/GuestHouse/downloaded_images'
OBJECT_STORAGE_URL = 'https://kr.object.ncloudstorage.com/gusethouse'

def extract_original_url(naver_cdn_url):
    """네이버 CDN URL에서 원본 이미지 URL 추출"""
    if 'src=' not in naver_cdn_url:
        return naver_cdn_url
    
    try:
        src_part = naver_cdn_url.split('src=')[1]
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

def download_table_images(cursor, table_name, id_column, url_column, folder):
    """테이블의 이미지 다운로드"""
    print(f"\n{'='*50}")
    print(f"테이블: {table_name} -> 폴더: {folder}")
    print(f"{'='*50}")
    
    # 다운로드 폴더 생성
    download_folder = os.path.join(DOWNLOAD_DIR, folder)
    os.makedirs(download_folder, exist_ok=True)
    
    # 네이버 CDN URL만 선택
    cursor.execute(f"""
        SELECT {id_column}, {url_column} 
        FROM {table_name} 
        WHERE {url_column} LIKE '%pstatic.net%'
        OR {url_column} LIKE '%search.pstatic.net%'
    """)
    
    rows = cursor.fetchall()
    total = len(rows)
    print(f"다운로드 대상: {total}개\n")
    
    # 매핑 정보 저장용
    mapping = []
    success_count = 0
    fail_count = 0
    
    for i, (row_id, old_url) in enumerate(rows, 1):
        print(f"[{i}/{total}] ID={row_id}")
        
        if not old_url:
            print("  URL 없음, 스킵")
            continue
        
        # 원본 URL 추출
        original_url = extract_original_url(old_url)
        
        # 확장자 결정
        ext = get_extension(original_url)
        
        # 파일명 생성 (ID 기반)
        filename = f"{row_id}.{ext}"
        filepath = os.path.join(download_folder, filename)
        
        # 이미 다운로드되었으면 스킵
        if os.path.exists(filepath):
            print(f"  이미 존재: {filename}")
            new_url = f"{OBJECT_STORAGE_URL}/{folder}/{filename}"
            mapping.append({
                'id': row_id,
                'old_url': old_url,
                'new_url': new_url,
                'filename': filename
            })
            success_count += 1
            continue
        
        # 이미지 다운로드
        image_data = download_image(original_url)
        if not image_data:
            # 원본 URL 실패 시 CDN URL로 시도
            image_data = download_image(old_url)
            if not image_data:
                print("  다운로드 실패")
                fail_count += 1
                continue
        
        # 파일 저장
        with open(filepath, 'wb') as f:
            f.write(image_data)
        
        # 매핑 정보 저장
        new_url = f"{OBJECT_STORAGE_URL}/{folder}/{filename}"
        mapping.append({
            'id': row_id,
            'old_url': old_url,
            'new_url': new_url,
            'filename': filename
        })
        
        print(f"  ✓ 저장됨: {filename} ({len(image_data)/1024:.1f}KB)")
        success_count += 1
        
        time.sleep(0.05)
    
    print(f"\n{table_name} 완료: 성공 {success_count}, 실패 {fail_count}")
    return mapping

def main():
    print("="*60)
    print("1단계: 이미지 다운로드")
    print(f"저장 위치: {DOWNLOAD_DIR}")
    print("="*60)
    
    # 다운로드 폴더 생성
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
    
    # DB 연결
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    all_mappings = {}
    
    try:
        # 1. accommodation_image 테이블
        mapping = download_table_images(
            cursor,
            table_name='accommodation_image',
            id_column='image_id',
            url_column='image_url',
            folder='accommodation_image'
        )
        all_mappings['accommodation_image'] = mapping
        
        # 2. room 테이블
        mapping = download_table_images(
            cursor,
            table_name='room',
            id_column='room_id',
            url_column='main_image_url',
            folder='room'
        )
        all_mappings['room'] = mapping
        
    finally:
        cursor.close()
        conn.close()
    
    # 매핑 정보 저장
    mapping_file = os.path.join(DOWNLOAD_DIR, 'mapping.json')
    with open(mapping_file, 'w', encoding='utf-8') as f:
        json.dump(all_mappings, f, ensure_ascii=False, indent=2)
    
    # SQL 파일 생성
    sql_file = os.path.join(DOWNLOAD_DIR, 'update_urls.sql')
    with open(sql_file, 'w', encoding='utf-8') as f:
        f.write("-- Cyberduck으로 업로드 완료 후 이 SQL 실행\n\n")
        
        for item in all_mappings.get('accommodation_image', []):
            f.write(f"UPDATE accommodation_image SET image_url = '{item['new_url']}' WHERE image_id = {item['id']};\n")
        
        f.write("\n")
        
        for item in all_mappings.get('room', []):
            f.write(f"UPDATE room SET main_image_url = '{item['new_url']}' WHERE room_id = {item['id']};\n")
    
    print("\n" + "="*60)
    print("다운로드 완료!")
    print(f"이미지 폴더: {DOWNLOAD_DIR}")
    print(f"SQL 파일: {sql_file}")
    print("="*60)
    print("\n다음 단계:")
    print("1. Cyberduck으로 아래 폴더를 Object Storage에 업로드:")
    print(f"   - {DOWNLOAD_DIR}/accommodation_image -> gusethouse/accommodation_image")
    print(f"   - {DOWNLOAD_DIR}/room -> gusethouse/room")
    print("2. 업로드 완료 후 update_urls.sql 실행")

if __name__ == '__main__':
    main()
