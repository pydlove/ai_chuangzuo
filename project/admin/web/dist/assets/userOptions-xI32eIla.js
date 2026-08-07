import{r as e}from"./request-DRGce7cr.js";function a(t="",r=20){return e.get("/users/options",{params:{keyword:t,limit:r}}).then(s=>s.data||[])}export{a as l};
