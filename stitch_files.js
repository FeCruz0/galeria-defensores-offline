const fs = require('fs');

const targetPath = 'app/src/main/java/com/galeria/defensores/data/GaidenData.kt';
const sourcePath = 'GaidenData_generated.kt';

const targetContent = fs.readFileSync(targetPath, 'utf8');
const sourceContent = fs.readFileSync(sourcePath, 'utf8');

// Find the start and end of getAdvantages in target
// It starts with "private fun getAdvantages(): MutableList<ItemDefinition> {"
// It ends with "}" before "private fun getDisadvantages"

const startMarker = 'private fun getAdvantages(): MutableList<ItemDefinition> {';
const endMarker = 'private fun getDisadvantages(): MutableList<ItemDefinition> {';

const startIndex = targetContent.indexOf(startMarker);
const endIndex = targetContent.indexOf(endMarker);

if (startIndex === -1 || endIndex === -1) {
    console.error("Could not find markers in target file.");
    process.exit(1);
}

// We need to find the closing brace of getAdvantages.
// Since getDisadvantages follows it (hopefully), we can just replace everything from startIndex up to (but not including) the line before getDisadvantages?
// Using the indentation, getDisadvantages is at root level of object (tabbed).
// Let's look for the last closing brace before getDisadvantages?
// Actually, let's just use the fact that getAdvantages is a block.
// But simpler: We have the full new function in sourceContent.
// We just need to replace the old function.

// Regex to match the function:
// private fun getAdvantages(): MutableList<ItemDefinition> \{[\s\S]*?^\s*\}
// But regex with generated content is risky.

// Let's just find where it starts, and find where getDisadvantages starts.
// The space between them is the function body + closing brace + some whitespace.
// In GaidenData.kt:
// 29:     private fun getAdvantages(): MutableList<ItemDefinition> {
// ...
// 92:     }
// 93: 
// 94:     private fun getDisadvantages(): MutableList<ItemDefinition> {

// So we remove from startIndex up to the line of getDisadvantages.
// But we need to keep `private fun getDisadvantages...`.
// So we replace [startIndex, endIndex) with sourceContent + "\n\n    ".

// Wait, sourceContent has "private fun getAdvantages... }" fully.
// So:
const pre = targetContent.substring(0, startIndex);
// We need to find exactly where to cut in target.
// We cut from startIndex.
// We should cut up to `private fun getDisadvantages`.
// But we need to be careful about what's between `}` of getAdvantages and `private fun getDisadvantages`.
// Usually it's just `\n\n    `.
// Let's assume we replace everything from startIndex until we see `private fun getDisadvantages`.

const newContent = pre + sourceContent + "\n\n    " + targetContent.substring(endIndex);

fs.writeFileSync(targetPath, newContent, 'utf8');
console.log("Files stitched successfully.");
