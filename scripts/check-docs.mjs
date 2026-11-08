// Verifica (1) que os links relativos dos arquivos Markdown de docs/, openspec/ e README resolvem e
// (2) que todo ADR de docs/adr/ consta no índice docs/adr/README.md.
// O template de ADR é ignorado (contém link de exemplo). Uso: pnpm docs:check
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, resolve } from "node:path";

const raiz = resolve(import.meta.dirname, "..");
const ignorados = new Set(["node_modules", ".git", "target", "dist", "estudo", "archive"]);
const markdowns = [];

function varrer(dir) {
  for (const nome of readdirSync(dir)) {
    if (ignorados.has(nome)) continue;
    const caminho = join(dir, nome);
    if (statSync(caminho).isDirectory()) varrer(caminho);
    else if (nome.endsWith(".md") && nome !== "0000-template.md") markdowns.push(caminho);
  }
}
for (const pasta of ["docs", "openspec"]) if (existsSync(join(raiz, pasta))) varrer(join(raiz, pasta));
if (existsSync(join(raiz, "README.md"))) markdowns.push(join(raiz, "README.md"));

const quebrados = [];
for (const arquivo of markdowns) {
  const texto = readFileSync(arquivo, "utf8").replace(/```[\s\S]*?```/g, "");
  for (const [, alvo] of texto.matchAll(/\]\(([^)\s]+)\)/g)) {
    if (/^(https?:|mailto:|#)/.test(alvo)) continue;
    const caminho = resolve(dirname(arquivo), alvo.split("#")[0]);
    if (!existsSync(caminho)) quebrados.push(`${arquivo.replace(raiz + "/", "")} -> ${alvo}`);
  }
}
const indice = readFileSync(join(raiz, "docs/adr/README.md"), "utf8");
const foraDoIndice = readdirSync(join(raiz, "docs/adr"))
  .filter((nome) => /^\d{4}-.+\.md$/.test(nome) && nome !== "0000-template.md" && !indice.includes(`(${nome})`))
  .map((nome) => `docs/adr/${nome}`);

if (quebrados.length) console.error("Links quebrados:\n" + quebrados.join("\n"));
if (foraDoIndice.length) console.error("ADRs fora do índice docs/adr/README.md:\n" + foraDoIndice.join("\n"));
if (quebrados.length || foraDoIndice.length) process.exit(1);
console.log(`${markdowns.length} arquivos Markdown, todos os links resolvem.`);
