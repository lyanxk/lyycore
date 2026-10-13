(function () {
    const fs = require('fs');
    const repo = 'F:/CodeMisc/Java/lyycore';
    const asset = JSON.parse(fs.readFileSync(repo + '/src/main/resources/assets/lyycore/geometry/emberfall.json', 'utf8'));
    if (Project.name !== 'Emberfall') throw Error('Open the Emberfall source model first');
    const source = JSON.parse(Codecs.project.compile());
    const previous = {mode: Modes.selected.id, slot: DisplayMode.display_slot, clip: Animation.selected?.name, time: Timeline.time};
    const contexts = ['firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'];
    const results = [];
    function sample(keys, time, fallback) {
        if (!keys?.length) return [fallback, fallback, fallback];
        if (time <= keys[0][0]) return keys[0].slice(1,4);
        let hi = keys.findIndex(k => k[0] >= time);
        if (hi < 0) return keys.at(-1).slice(1,4);
        const lo = hi-1, a = keys[lo], b = keys[hi], t = (time-a[0])/(b[0]-a[0]);
        if (a[4] === 1 && time < b[0]) return a.slice(1,4);
        return [1,2,3].map(axis => {
            if (a[4] === 2 || b[4] === 2) {
                const p0=keys[Math.max(0,lo-1)][axis], p1=a[axis], p2=b[axis], p3=keys[Math.min(keys.length-1,hi+1)][axis];
                return .5*(2*p1+(-p0+p2)*t+(2*p0-5*p1+4*p2-p3)*t*t+(-p0+3*p1-3*p2+p3)*t*t*t);
            }
            return a[axis]+(b[axis]-a[axis])*t;
        });
    }
    function runtimeMatrices(clip, time, display, left, centerY) {
        const d = Math.PI/180, sign = left ? -1 : 1;
        const base = new THREE.Matrix4().compose(
            new THREE.Vector3(display.translation[0]*sign,display.translation[1],display.translation[2]),
            new THREE.Quaternion().setFromEuler(new THREE.Euler(display.rotation[0]*d,display.rotation[1]*d*sign,display.rotation[2]*d*sign,'XYZ')),
            new THREE.Vector3(...display.scale)
        ).multiply(new THREE.Matrix4().makeTranslation(0,centerY,0));
        const matrices=[];
        asset.bones.forEach((b,i)=>{
            const tracks=clip.tracks[i]||{}, p=sample(tracks.position,time,0), r=sample(tracks.rotation,time,0), s=sample(tracks.scale,time,1);
            const parent=b.parent<0?[0,0,0]:asset.bones[b.parent].pivot;
            const local=new THREE.Matrix4().compose(new THREE.Vector3(...p.map((v,j)=>v+b.pivot[j]-parent[j])),
                new THREE.Quaternion().setFromEuler(new THREE.Euler(...r.map((v,j)=>(v+b.rotation[j])*d),'ZYX')),new THREE.Vector3(...s));
            matrices.push((b.parent<0?base:matrices[b.parent]).clone().multiply(local));
        });
        return matrices;
    }
    try {
        Formats.free.display_mode=true; Modes.options.display.select();
        for (const context of contexts) {
            DisplayMode.load(context);
            let oldError=0, fixedError=0, count=0;
            for (const [name,times] of [['idle',[0,.5]],['block_enter',[0,.15,.3]],['block_hold',[0,.5]],['heavy_attack',[0,.4,.9,1.5]]]) {
                const animation=Animation.all.find(a=>a.name.endsWith('.'+name)); animation.select();
                for (const t of times) {
                    Timeline.setTime(t); Animator.preview(); DisplayMode.display_area.updateMatrixWorld(true);
                    const inverse=new THREE.Matrix4().copy(DisplayMode.display_area.matrixWorld).invert();
                    const old=runtimeMatrices(asset.animations[name],t,source.display[context],context.includes('lefthand'),0);
                    const fixed=runtimeMatrices(asset.animations[name],t,source.display[context],context.includes('lefthand'),-8);
                    asset.bones.forEach((b,i)=>{
                        if (b.name.startsWith('energy_')) return;
                        const g=Group.all.find(g=>g.name===b.name); const native=inverse.clone().multiply(g.mesh.matrixWorld);
                        oldError=Math.max(oldError,...native.elements.map((v,j)=>Math.abs(v-old[i].elements[j])));
                        fixedError=Math.max(fixedError,...native.elements.map((v,j)=>Math.abs(v-fixed[i].elements[j])));
                        count++;
                    });
                }
            }
            results.push({context,count,oldMaxErrorBBUnits:oldError,fixedMaxErrorBBUnits:fixedError});
        }
    } finally {
        if (previous.mode==='display') DisplayMode.load(previous.slot);
        else Modes.options[previous.mode].select();
        const animation=Animation.all.find(a=>a.name===previous.clip); if(animation) animation.select();
        Timeline.setTime(previous.time); Animator.preview();
    }
    const report={reference:'Live Blockbench native Display matrices, including model Y=-8 centering',results};
    fs.writeFileSync(repo+'/tools/emberfall/display_alignment_validation.json',JSON.stringify(report,null,2));
    if (results.some(r=>r.fixedMaxErrorBBUnits>1e-5)) throw Error('Emberfall runtime transform no longer matches the native Blockbench Display preview');
    return report;
})()
