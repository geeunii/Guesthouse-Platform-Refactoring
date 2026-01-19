"""
썸네일 생성 스크립트
원본 이미지를 400x400 썸네일로 리사이즈

폴더 구조:
- downloaded_images/accommodation_image/ (원본)
- downloaded_images/accommodation_image_thumb/ (썸네일)
- downloaded_images/room/ (원본)
- downloaded_images/room_thumb/ (썸네일)
"""

from PIL import Image
import os

# 설정
SOURCE_DIR = 'd:/GuestHouse/downloaded_images'
THUMBNAIL_SIZE = (400, 400)  # 카드에서 200px로 표시되므로 2배 해상도

def create_thumbnail(source_path, dest_path, size):
    """이미지를 썸네일로 변환"""
    try:
        with Image.open(source_path) as img:
            # RGBA를 RGB로 변환 (PNG 알파 채널 처리)
            if img.mode in ('RGBA', 'LA', 'P'):
                background = Image.new('RGB', img.size, (255, 255, 255))
                if img.mode == 'P':
                    img = img.convert('RGBA')
                background.paste(img, mask=img.split()[-1] if img.mode == 'RGBA' else None)
                img = background
            elif img.mode != 'RGB':
                img = img.convert('RGB')
            
            # 정사각형 크롭 (중앙 기준)
            width, height = img.size
            min_dim = min(width, height)
            left = (width - min_dim) // 2
            top = (height - min_dim) // 2
            right = left + min_dim
            bottom = top + min_dim
            img = img.crop((left, top, right, bottom))
            
            # 리사이즈 (고품질)
            img = img.resize(size, Image.Resampling.LANCZOS)
            
            # 저장 (JPEG 품질 90%)
            img.save(dest_path, 'JPEG', quality=90, optimize=True)
            return True
    except Exception as e:
        print(f"  실패: {e}")
        return False

def process_folder(source_folder, dest_folder):
    """폴더의 모든 이미지를 썸네일로 변환"""
    if not os.path.exists(source_folder):
        print(f"폴더 없음: {source_folder}")
        return 0, 0
    
    os.makedirs(dest_folder, exist_ok=True)
    
    files = [f for f in os.listdir(source_folder) if f.lower().endswith(('.jpg', '.jpeg', '.png', '.gif', '.webp'))]
    total = len(files)
    success = 0
    fail = 0
    
    print(f"\n{'='*50}")
    print(f"원본: {source_folder}")
    print(f"썸네일: {dest_folder}")
    print(f"총 {total}개 파일")
    print(f"{'='*50}")
    
    for i, filename in enumerate(files, 1):
        source_path = os.path.join(source_folder, filename)
        
        # 썸네일은 항상 jpg로 저장
        thumb_filename = os.path.splitext(filename)[0] + '.jpg'
        dest_path = os.path.join(dest_folder, thumb_filename)
        
        # 이미 존재하면 스킵
        if os.path.exists(dest_path):
            print(f"[{i}/{total}] {filename} - 이미 존재")
            success += 1
            continue
        
        if create_thumbnail(source_path, dest_path, THUMBNAIL_SIZE):
            file_size = os.path.getsize(dest_path) / 1024
            print(f"[{i}/{total}] {filename} -> {thumb_filename} ({file_size:.1f}KB)")
            success += 1
        else:
            fail += 1
    
    print(f"\n완료: 성공 {success}, 실패 {fail}")
    return success, fail

def main():
    print("="*60)
    print("썸네일 생성 스크립트")
    print(f"크기: {THUMBNAIL_SIZE[0]}x{THUMBNAIL_SIZE[1]}px")
    print("="*60)
    
    total_success = 0
    total_fail = 0
    
    # 1. accommodation_image 썸네일
    s, f = process_folder(
        os.path.join(SOURCE_DIR, 'accommodation_image'),
        os.path.join(SOURCE_DIR, 'accommodation_image_thumb')
    )
    total_success += s
    total_fail += f
    
    # 2. room 썸네일
    s, f = process_folder(
        os.path.join(SOURCE_DIR, 'room'),
        os.path.join(SOURCE_DIR, 'room_thumb')
    )
    total_success += s
    total_fail += f
    
    print("\n" + "="*60)
    print(f"전체 완료: 성공 {total_success}, 실패 {total_fail}")
    print("="*60)
    print("\n다음 단계:")
    print("1. Cyberduck으로 썸네일 폴더를 Object Storage에 업로드:")
    print(f"   - {SOURCE_DIR}/accommodation_image_thumb -> guesthouse/accommodation_image_thumb")
    print(f"   - {SOURCE_DIR}/room_thumb -> guesthouse/room_thumb")
    print("2. 업로드 완료 후 알려주세요!")

if __name__ == '__main__':
    main()
