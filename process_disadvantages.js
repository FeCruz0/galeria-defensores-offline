const fs = require('fs');

const inputFile = 'disadvantages_raw.txt';
const outputFile = 'GaidenData_disadvantages_generated.kt';

let rawText = fs.readFileSync(inputFile, 'utf8');

// Normalize en-dashes to hyphens because text might have mixed usage
rawText = rawText.replace(/–/g, '-');

// Replacements
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

// Parsing
const lines = processedText.split('\n');
let kotlinCode = ""; 
// Removing the function header wrapper to assume we will just insert list.add lines? 
// Or better to keep it and splice the whole function. 
// The plan said "replace body". If I generate the whole function, I can splice it easily.
kotlinCode += "    private fun getDisadvantages(): MutableList<ItemDefinition> {\n";
kotlinCode += "        val list = mutableListOf<ItemDefinition>()\n\n";

let currentName = "";
let currentBody = "";

function addEntry(name, body) {
    let cost = "-1";
    
    // Find all occurrences of -XPT
    // Note: since we normalized –, we just look for -
    const regex = /-(\d+)PT/g;
    let match;
    const costs = new Set();
    
    while ((match = regex.exec(body)) !== null) {
        costs.add(parseInt(match[1]));
    }

    if (costs.size > 0) {
        const sortedCosts = Array.from(costs).sort((a, b) => a - b); // 1, 2, 3
        if (sortedCosts.length === 1) {
            cost = `-${sortedCosts[0]}`;
        } else {
            // Range: -1 a -3 (smallest number first in magnitude, but logically 1 to 3 points of disadvantage)
            cost = `-${sortedCosts[0]} a -${sortedCosts[sortedCosts.length - 1]}`;
        }
    } else {
        // Fallback or specific manual overrides
        if (body.includes("Desvantagem Suave")) cost = "-1";
        if (body.includes("Desvantagem Grave")) cost = "-2";
        if (body.includes("Desvantagem Letal")) cost = "-3";
        
        // Manual checks if needed
        if (name === "MUNIÇÃO LIMITADA") cost = "-1"; 
    }

    // Escape quotes and newlines for Kotlin string
    const bodyEscaped = body.replace(/"/g, '\\"').replace(/\n/g, '\\n');
    const bodyFinal = bodyEscaped.replace(/\$/g, '\\$');
    
    kotlinCode += `        list.add(ItemDefinition(name = "${name}", cost = "${cost}", description = "${bodyFinal}"))\n`;
}

for (let i = 0; i < lines.length; i++) {
    let line = lines[i].trim();
    if (!line) continue;

    // Header detection: Uppercase, > 2 chars, starts with letter
    // Exclude specific abbreviated terms that might appear in caps
    const isHeader = (line.toUpperCase() === line && line.length > 2 && /^[A-Z]/.test(line));
    
    const ignoredHeaders = ["ATV", "PSV", "TEC", "JNT", "PRT", "LNG", "FDA", "ACT", "MOV", "INI", "CNA", "STN", "EQP", "PDR", "PRC"];
    // Also "1PT", "-1PT" etc are not headers (handled by regex start with letter)

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
console.log("Kotlin code for disadvantages generated successfully.");
