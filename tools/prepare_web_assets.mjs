import {createRequire} from 'node:module';
import {readdir,writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const require=createRequire(import.meta.url);
const sharp=require(process.env.LUMA_SHARP_PATH || 'sharp');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const assets=path.join(root,'website','dist','assets');
const concept=path.join(root,'output','lobby-concept');
const inputs=[['city',path.join(concept,'lobby-compact-fantasy.png')],['luma-logo',path.join(concept,'brand','luma-logo.png')],...['item-aether-halo','item-runeblade'].map(id=>[id,path.join(concept,'assets','references',`${id}.png`)]),...(await readdir(path.join(concept,'zones'))).filter(f=>f.endsWith('.png')).map(f=>[f.replace(/\.png$/,''),path.join(concept,'zones',f)])];
// Format/compression only: keep image dimensions and original art unchanged.
for(const [id,source] of inputs) await sharp(source).webp({quality:84,effort:5}).toFile(path.join(assets,`${id}.webp`));
for(const name of ['index.html','app.js','portal.js']){
 const fs=await import('node:fs/promises');const p=path.join(root,'website','dist',name);
 await writeFile(p,(await fs.readFile(p,'utf8')).replace(/\.png/g,'.webp'));
}
console.log(`Compressed ${inputs.length} web assets. Original art remains in output/lobby-concept.`);
