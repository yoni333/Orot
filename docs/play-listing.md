# Play Store listing text — Orot

Copy-paste into Play Console → Grow → Store presence → Main store listing.
Default language: **Hebrew (עברית)**.

---

## App name (max 30)

```
אורות — הראי״ה קוק
```

The in-app `app_name` is `Orot (אורות)`. The Play listing name does not have to
match it, and a Hebrew-first name reads better to the actual audience.

---

## Short description (max 80)

```
ספר אורות של הראי״ה קוק. קריאה, סימון, הערות וחיפוש — הכל עובד בלי אינטרנט.
```

---

## Full description (max 4000)

```
ספר "אורות" של הראי״ה קוק זצ״ל, בממשק קריאה נקי ונוח בעברית.

כל הספר — 146 פרקים — ארוז בתוך האפליקציה. אין צורך בחיבור לאינטרנט, אין הרשמה ואין חשבון. פותחים וקוראים.

■ קריאה
• טקסט מלא של הספר: ארץ ישראל, המלחמה, ישראל ותחייתו, אורות התחיה, למהלך האידיאות, זרעונים, אורות ישראל ועוד.
• תוכן עניינים מלא לניווט מהיר בין הפרקים.
• שלוש ערכות גופן — מודרני, קלאסי ומכונת כתיבה.
• שליטה בגודל הטקסט.
• מצב לילה לקריאה נוחה בחושך.
• עיצוב מותאם לעברית מימין לשמאל.

■ לימוד וסימון
• סימניות לפרקים ולפסקאות בודדות.
• הדגשת קטעי טקסט בצבעים.
• הערות אישיות על כל פסקה.
• ייצוא כל ההערות לקובץ Word ושיתוף שלו — נוח להכנת שיעור או סיכום.

■ חיפוש
• חיפוש חופשי בכל הספר.
• שמירת חיפושים אחרונים לחזרה מהירה.

■ פרטיות
האפליקציה אינה אוספת שום מידע. הסימניות, ההדגשות וההערות נשמרות רק במכשיר שלך ואינן נשלחות לשום מקום. אין פרסומות, אין אנליטיקה ואין מעקב.

■ קרדיט
תוכן הספר הועתק מוויקיטקסט והוא תחת רישיון Creative Commons ייחוס־שיתוף זהה 4.0.

להצעות, הערות ובקשות להוספת ספרים: yoni333@gmail.com
```

---

## Release notes — internal testing, first release

```
גרסה ראשונה לבדיקה פנימית.
ספר אורות המלא, חיפוש, סימניות, הדגשות, הערות אישיות וייצוא לוורד.
```

---

## Other fields

| Field | Value |
|---|---|
| Category | Books & Reference |
| Tags | Religion, Reading |
| Contact email | yoni333@gmail.com |
| Privacy policy URL | https://yoni333.github.io/Orot/privacy-policy.html |
| Ads | No |
| In-app purchases | No |
| Content rating | Everyone (religious text, no user-generated content shared) |
| App access | All functionality available without login — state this in the App access form |

### Data safety answers

- Does your app collect or share any user data? → **No**
- Is all user data encrypted in transit? → n/a (nothing is transmitted)
- Do you provide a way to request data deletion? → n/a; note that uninstalling
  the app removes everything, since all data is local.

The app declares `INTERNET` and `ACCESS_NETWORK_STATE` but makes no network
calls — the full text loads from `app/src/main/assets/orot_data.json`. Declaring
"no data collected" is accurate.
