import{r as a}from"./request-DDp2kH_P.js";function i(t="",r=20){return a.get("/api/v1/admin/users/options",{params:{keyword:t,limit:r}}).then(s=>s.data||[])}export{i as l};
