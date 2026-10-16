import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_analyze_prompt = '''                val promptText = if (locationContext != null) {
                    "Bu bitkileri analiz et. Bulunduğum konum: $locationContext. Konuma göre bitki teşhisini ve bu bölgedeki endemik/yaygın durumunu da dikkate al."
                } else {
                    "Bu bitkileri analiz et."
                }'''

new_analyze_prompt = '''                val promptText = if (locationContext != null) {
                    "Lütfen bu bitkileri analiz et ve teşhisini yap. Bulunduğum il/ilçe konumu: $locationContext. Konuma göre bitki teşhisini ve bu bölgedeki endemik/yaygın durumunu dikkate al. Bu bitkilerin belirgin özelliklerini, başka hangi bölgelerde yetiştiğini ve ne işe yaradıklarını (faydalarını) detaylıca Türkçe olarak açıkla."
                } else {
                    "Lütfen bu bitkileri analiz et ve teşhisini yap. Belirgin özelliklerini, nerelerde yetiştiğini ve ne işe yaradıklarını detaylıca Türkçe olarak açıkla."
                }'''

content = content.replace(old_analyze_prompt, new_analyze_prompt)

old_discover_prompt = '''                val promptText = "Bulunduğum bölge: $locationContext. Bu bölgede yetişen önemli, endemik veya yaygın bitkileri, faydalarını ve özelliklerini liste biçiminde anlat."'''

new_discover_prompt = '''                val promptText = "Bulunduğum il ve ilçe konumu: $locationContext. Sadece bulunduğum ülkeye göre değil, doğrudan bu il ve ilçe sınırları içerisinde yetişen bitkilere odaklan. Bu bölgeye özgü (endemik) olan veya yaygın olarak bulunan önemli bitkileri listele. Bu bitkilerin başka nerelerde yetiştiğini, belirgin özelliklerini ve ne işe yaradıklarını (faydalarını) liste biçiminde detaylıca Türkçe olarak anlat."'''

content = content.replace(old_discover_prompt, new_discover_prompt)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
