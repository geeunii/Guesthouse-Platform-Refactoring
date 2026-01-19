"""
DB 구조에 맞게 NCP Object Storage에 직접 업로드
기존 accommodation_image/숫자/ 폴더 구조 유지
"""

import os
import boto3
import mysql.connector
from botocore.config import Config

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

NCLOUD_CONFIG = {
    'endpoint': os.environ.get('NCP_ENDPOINT', 'https://kr.object.ncloudstorage.com'),
    'region': os.environ.get('NCP_REGION', 'kr-standard'),
    'bucket': os.environ.get('NCP_BUCKET', 'guesthouse'),
    'access_key': required_env('NCP_ACCESS_KEY'),
    'secret_key': required_env('NCP_SECRET_KEY')
}

SOURCE_DIR = 'resized_images/resizing_accommodation'

# S3 클라이언트
s3_client = boto3.client(
    's3',
    endpoint_url=NCLOUD_CONFIG['endpoint'],
    region_name=NCLOUD_CONFIG['region'],
    aws_access_key_id=NCLOUD_CONFIG['access_key'],
    aws_secret_access_key=NCLOUD_CONFIG['secret_key'],
    config=Config(signature_version='s3v4')
)

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    # DB에서 원본 URL 조회
    cursor.execute("""
        SELECT image_id, image_url 
        FROM accommodation_image 
        WHERE image_url LIKE '%accommodation_image/%'
    """)
    
    success = 0
    fail = 0
    
    for image_id, url in cursor.fetchall():
        # URL에서 폴더와 파일명 추출
        # 예: .../accommodation_image/3/banner_0_11.jpg
        parts = url.split('/')
        if len(parts) >= 3:
            folder_num = parts[-2]  # "3"
            filename = parts[-1]    # "banner_0_11.jpg"
            
            # 로컬 파일 경로
            local_file = os.path.join(SOURCE_DIR, filename)
            
            if not os.path.exists(local_file):
                print(f"✗ {filename} not found locally")
                fail += 1
                continue
            
            # NCP 경로 (원본 구조 유지)
            ncp_key = f"accommodation_image/{folder_num}/{filename}"
            
            try:
                # 업로드
                with open(local_file, 'rb') as f:
                    s3_client.put_object(
                        Bucket=NCLOUD_CONFIG['bucket'],
                        Key=ncp_key,
                        Body=f,
                        ContentType='image/jpeg',
                        ACL='public-read'
                    )
                print(f"✓ {ncp_key}")
                success += 1
            except Exception as e:
                print(f"✗ {ncp_key}: {e}")
                fail += 1
    
    cursor.close()
    conn.close()
    
    print(f"\n완료! 성공: {success}, 실패: {fail}")
    print("DB URL 변경 없이 기존 구조 그대로 유지됩니다.")

if __name__ == '__main__':
    main()
