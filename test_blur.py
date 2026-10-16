import urllib.request
url = "https://images.unsplash.com/photo-1506260408121-e353d10b87c7?q=80&w=800&auto=format&fit=crop&blur=100"
urllib.request.urlretrieve(url, "app/src/main/res/drawable/bg_grass_blurred.jpg")
