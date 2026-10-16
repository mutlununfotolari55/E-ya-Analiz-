import re

with open('app/src/main/java/com/example/LocationHelper.kt', 'r') as f:
    content = f.read()

new_location_logic = """                val address = addresses[0]
                return buildString {
                    if (address.subAdminArea != null) append(address.subAdminArea).append(", ")
                    if (address.adminArea != null) append(address.adminArea).append(", ")
                    if (address.countryName != null) append(address.countryName)
                }.trimEnd(',', ' ')"""

old_location_logic = """                val address = addresses[0]
                return buildString {
                    if (address.locality != null) append(address.locality).append(", ")
                    if (address.adminArea != null) append(address.adminArea).append(", ")
                    if (address.countryName != null) append(address.countryName)
                }.trimEnd(',', ' ')"""

content = content.replace(old_location_logic, new_location_logic)

with open('app/src/main/java/com/example/LocationHelper.kt', 'w') as f:
    f.write(content)
