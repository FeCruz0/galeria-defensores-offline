const fs = require('fs');

const targetPath = 'app/src/main/java/com/galeria/defensores/data/GaidenData.kt';
const sourcePath = 'GaidenData_disadvantages_generated.kt';

const targetContent = fs.readFileSync(targetPath, 'utf8');
const sourceContent = fs.readFileSync(sourcePath, 'utf8');

// Markers
const startMarker = 'private fun getDisadvantages(): MutableList<ItemDefinition> {';
const endMarker = 'private fun getSkills(): MutableList<ItemDefinition> {';

const startIndex = targetContent.indexOf(startMarker);
const endIndex = targetContent.indexOf(endMarker);

if (startIndex === -1 || endIndex === -1) {
    console.error("Could not find markers in target file.");
    // Fallback: if getSkills is not found (maybe it's the last function?), try finding the end of the class or just looking for the closing brace of the function if possible.
    // But based on previous reads, getSkills should be there.
    process.exit(1);
}

const pre = targetContent.substring(0, startIndex);
const post = targetContent.substring(endIndex);

// sourceContent contains the full function including closing brace.
// We need to ensure we don't mess up newlines.
const newContent = pre + sourceContent + "\n\n    " + post;

fs.writeFileSync(targetPath, newContent, 'utf8');
console.log("GaidenData.kt updated with disadvantages.");
