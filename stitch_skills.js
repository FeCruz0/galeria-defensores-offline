const fs = require('fs');

const targetPath = 'app/src/main/java/com/galeria/defensores/data/GaidenData.kt';
const sourcePath = 'GaidenData_skills_generated.kt';

const targetContent = fs.readFileSync(targetPath, 'utf8');
const sourceContent = fs.readFileSync(sourcePath, 'utf8');

// Marker
const startMarker = 'private fun getSkills(): MutableList<ItemDefinition> {';

const startIndex = targetContent.lastIndexOf(startMarker);

if (startIndex === -1) {
    console.error("Could not find start marker in target file.");
    process.exit(1);
}

// We assume getSkills is the last function in the object.
// So we take everything before getSkills, append the new getSkills, and then valid closing brace for the object.
const pre = targetContent.substring(0, startIndex);

// Ensure there is a closing brace for the object.
// The sourceContent ends with a newline and a brace for the function.
// We need to add the closing brace for the class/object.
const newContent = pre + sourceContent + "\n}\n";

fs.writeFileSync(targetPath, newContent, 'utf8');
console.log("GaidenData.kt updated with skills.");
