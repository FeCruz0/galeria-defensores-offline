const fs = require('fs');

const inputFile = 'skills_raw.txt';
const outputFile = 'GaidenData_skills_generated.kt';

let rawText = fs.readFileSync(inputFile, 'utf8');

// Normalize en-dashes to hyphens
rawText = rawText.replace(/–/g, '-');

// Replacements (Standard Gaiden abbreviations)
const replacements = {
    "\\bJNT\\b": "Junto",
    "\\bPRT\\b": "Perto",
    "\\bLNG\\b": "Longe",
    "\\bFDA\\b": "Fora do Alcance",
    "\\bACT\\b": "Ação",
    "\\bMOV\\b": "Movimento",
    "\\bINI\\b": "Iniciativa",
    "\\bCNA\\b": "Cena",
    "\\bSTN\\b": "Status Negativo",
    "\\bEQP\\b": "Equipamento",
    "\\bPDR\\b": "Poder",
    "\\bPRC\\b": "Perícia",
    "\\bATV\\b": "Poder Ativo",
    "\\bPSV\\b": "Poder Passivo",
    "\\bTEC\\b": "Técnica"
};

let processedText = rawText;

for (const [abbr, fullWord] of Object.entries(replacements)) {
    const regex = new RegExp(abbr, 'g');
    processedText = processedText.replace(regex, fullWord);
}

const lines = processedText.split('\n');
let kotlinCode = "    private fun getSkills(): MutableList<ItemDefinition> {\n";
kotlinCode += "        val list = mutableListOf<ItemDefinition>()\n\n";

let currentName = "";
let currentBody = "";

function addEntry(name, body) {
    // Skills usually cost 1 or 2 points in 3DeT Victory/Gaiden.
    // If text doesn't specify, we default to "1-2" or just "2" or leave it to user interpretation?
    // In Gaiden (Victory based), Perícias are often 1 point for specific, 2 for broad.
    // These look like Broad Skills (Animais, Artes, etc).
    // I will set cost to "2" as a safe default for Broad Skills, or just "2".
    // Alternatively, I can look for "1PT" or "2PT" in the text, but the text has sub-uses costing points.
    // Let's default to "2".
    let cost = "2"; 
    
    // Escape quotes and newlines for Kotlin string
    const bodyEscaped = body.replace(/"/g, '\\"').replace(/\n/g, '\\n');
    const bodyFinal = bodyEscaped.replace(/\$/g, '\\$');
    
    kotlinCode += `        list.add(ItemDefinition(name = "${name}", cost = "${cost}", description = "${bodyFinal}"))\n`;
}

for (let i = 0; i < lines.length; i++) {
    let line = lines[i].trim();
    if (!line) continue;

    // Header detection: Uppercase, > 2 chars, starts with letter
    const isHeader = (line.toUpperCase() === line && line.length > 2 && /^[A-Z]/.test(line));
    
    // Ignored headers to avoid false positives
    const ignoredHeaders = ["ATV", "PSV", "TEC", "JNT", "PRT", "LNG", "FDA", "ACT", "MOV", "INI", "CNA", "STN", "EQP", "PDR", "PRC"];

    if (isHeader && !line.includes(":") && !ignoredHeaders.includes(line)) {
         if (currentName) {
             addEntry(currentName, currentBody);
         }
         currentName = line;
         currentBody = "";
    } else {
        currentBody += line + "\n";
    }
}

if (currentName) {
    addEntry(currentName, currentBody);
}

kotlinCode += "\n        return list\n    }\n";

fs.writeFileSync(outputFile, kotlinCode);
console.log("Kotlin code for skills generated successfully.");
