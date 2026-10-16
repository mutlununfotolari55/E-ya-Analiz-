from PIL import Image, ImageFilter
import os

img_path = "app/src/main/res/drawable/bg_grass.jpg"
if os.path.exists(img_path):
    img = Image.open(img_path)
    img = img.filter(ImageFilter.GaussianBlur(radius=15))
    img.save("app/src/main/res/drawable/bg_grass_blurred.jpg", quality=85)
    print("Blurred successfully")
else:
    print("Image not found")
