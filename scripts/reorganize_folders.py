"""
DB에서 폴더 구조를 읽어서 로컬에 폴더 구조 재생성
"""

import os
import shutil
import mysql.connector

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

SOURCE_DIR = 'resized_images/resizing_accommodation'
OUTPUT_DIR = 'resized_images/resizing_accommodation_structured'

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    # DB에서 원본 URL 조회
    cursor.execute("""
        SELECT image_id, image_url 
        FROM accommodation_image 
        WHERE image_url LIKE '%accommodation_image/%'
    """)
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    for image_id, url in cursor.fetchall():
        # URL에서 폴더 번호와 파일명 추출
        # 예: .../accommodation_image/3/banner_0_11.jpg
        parts = url.split('/')
        if len(parts) >= 3:
            folder_num = parts[-2]  # "3"
            filename = parts[-1]    # "banner_0_11.jpg"
            
            # 폴더 생성
            folder_path = os.path.join(OUTPUT_DIR, folder_num)
            os.makedirs(folder_path, exist_ok=True)
            
            # 파일 복사 (있으면)
            src = os.path.join(SOURCE_DIR, filename)
            dst = os.path.join(folder_path, filename)
            
            if os.path.exists(src):
                shutil.copy2(src, dst)
                print(f"✓ {folder_num}/{filename}")
            else:
                print(f"✗ {filename} not found")
    
    cursor.close()
    conn.close()
    
    print(f"\n완료! 구조화된 폴더: {OUTPUT_DIR}")
    print("Cyberduck으로 이 폴더를 NCP의 resizing_accommodation에 업로드하세요.")

if __name__ == '__main__':
    main()

