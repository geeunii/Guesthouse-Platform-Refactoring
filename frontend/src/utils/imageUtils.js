export const resizeImage = (file, maxWidth = 1024, maxHeight = 1024) => {
    return new Promise((resolve, reject) => {
        if (!file || !file.type.match(/image.*/)) {
            reject(new Error('파일이 이미지가 아닙니다.'))
            return
        }

        const reader = new FileReader()
        reader.onload = (readerEvent) => {
            const image = new Image()
            image.onload = () => {
                let width = image.width
                let height = image.height

                if (width > maxWidth || height > maxHeight) {
                    if (width > height) {
                        if (width > maxWidth) {
                            height *= maxWidth / width
                            width = maxWidth
                        }
                    } else {
                        if (height > maxHeight) {
                            width *= maxHeight / height
                            height = maxHeight
                        }
                    }
                }

                const canvas = document.createElement('canvas')
                canvas.width = width
                canvas.height = height
                const ctx = canvas.getContext('2d')
                ctx.drawImage(image, 0, 0, width, height)

                const dataUrl = canvas.toDataURL('image/jpeg', 0.8) // 0.8 quality
                resolve(dataUrl)
            }
            image.onerror = () => reject(new Error('이미지 로드 실패'))
            image.src = readerEvent.target.result
        }
        reader.onerror = () => reject(new Error('파일 읽기 실패'))
        reader.readAsDataURL(file)
    })
}
