import urllib.request

# Dark grass image from unsplash
url = "https://images.unsplash.com/photo-1506260408121-e353d10b87c7?q=80&w=800&auto=format&fit=crop"
try:
    urllib.request.urlretrieve(url, "app/src/main/res/drawable/bg_grass.jpg")
    print("Success")
except Exception as e:
    print("Error:", e)
