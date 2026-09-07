# Git Guide: Cricket Draft Project

Yeh guide aapko batayegi ke aapko apna Backend aur Mobile app ka code kahan aur kaise push karna hai, aur live server (cPanel) par kaise update karna hai. Dhyan se in steps ko follow karein.

## 1. Backend Code (Laravel) Push Karna

Aapka backend code root folder mein mojood hai. Isko `cricketbackend` repository mein push kiya jata hai.

**Folder Path:** `c:\Users\Muhammad Aliyan\Downloads\cricket-draft-source`
**GitHub Repo:** `https://github.com/ali786-c/cricketbackend.git`

**Push karne ke steps (VS Code Terminal mein root folder se):**
```bash
git add .
git commit -m "Yahan apne changes ke bare mein likhein"
git push origin main
```

---

## 2. Mobile App Code (Android/Kotlin) Push Karna

Mobile app ka code alag folder mein hai aur iski GitHub repository bhi alag hai (`neewmobileapp`). Ise backend wali repo mein push nahi karna!

**Folder Path:** `c:\Users\Muhammad Aliyan\Downloads\cricket-draft-source\cricket-draft-mobile`
**GitHub Repo:** `https://github.com/ali786-c/neewmobileapp.git`

**Push karne ke steps:**
Pehle aapko mobile app ke folder mein jana hoga:
```bash
cd cricket-draft-mobile
```
Phir push karein:
```bash
git add .
git commit -m "Mobile app mein kiye gaye changes ki details"
git push origin main
```
Wapas backend (root) folder mein aane ke liye:
```bash
cd ..
```

---

## 3. Live Server (cPanel) Par Code Pull Karna (Sirf Backend)

Jab aap apne computer se Backend ka code push kar lein, to live server par website update karne ke liye yeh steps follow karein:

1. Apne cPanel mein login karein.
2. **Terminal** open karein.
3. Apne project ke folder mein jayen (jaise `cricket.careerinpak.com`):
   ```bash
   cd path/to/your/folder
   ```
4. Naya code pull karein:
   ```bash
   git pull origin main
   ```
5. Laravel ke caches clear karein aur naye packages install karein:
   ```bash
   composer install --no-interaction --prefer-dist --optimize-autoloader
   php artisan optimize:clear
   ```
   *(Agar database tables mein koi naya change hai to `php artisan migrate` bhi chala lein).*

---
**Zaroori Hidayat (Important Note):** 
- Kabhi bhi root folder (backend) se mobile code push karne ki koshish na karein. Mobile ka code hamesha `cricket-draft-mobile` folder ke andar jaa kar push karein.
- cPanel par hamesha sirf Backend ka code pull hoga kyunki wahan live website chal rahi hai. Mobile app Google Playstore ya APK ke zariye update hoti hai.
