// Syntax gate for the generated canvas: parses the .tsx as TypeScript + JSX.
// This is not a type check (no tsc in the IDE-managed canvas project), but it
// catches the JSX/TS syntax errors that would blank the preview panel.
const fs = require("fs");
const parser = require("d:/Projects/Persion/ai-customer-service/ai-cs-frontend/node_modules/@babel/parser");

const file = process.argv[2];
const code = fs.readFileSync(file, "utf8");

try {
  const ast = parser.parse(code, {
    sourceType: "module",
    plugins: ["typescript", "jsx"],
  });
  const hasDefault = ast.program.body.some(
    (n) => n.type === "ExportDefaultDeclaration"
  );
  const imports = ast.program.body
    .filter((n) => n.type === "ImportDeclaration")
    .map((n) => n.source.value);
  console.log(`[OK] parsed ${file}`);
  console.log(`[OK] export default present: ${hasDefault}`);
  console.log(`[i]  import sources: ${JSON.stringify(imports)}`);
  const bad = imports.filter((s) => s !== "qoder/canvas");
  if (bad.length) {
    console.log(`[FAIL] non-canvas import: ${bad.join(", ")}`);
    process.exit(1);
  }
  if (!hasDefault) process.exit(1);
} catch (err) {
  console.log(`[FAIL] ${err.message}`);
  process.exit(1);
}
