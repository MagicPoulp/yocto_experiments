const fs = require('fs');
const express = require('express');
const router = express.Router();
const app = express();

router.get('/api/items', (req, res) => {
  //console.log("request\n");
  fs.readFile('/home/developer/tmp/geomatics_example3.json', (err, json) => {
    let obj = JSON.parse(json);
    res.json(obj);
  });
});

app.use(router)

const https = require('https')
const path = require('path')
const options = {
  key: fs.readFileSync(path.join(__dirname, "server.key")),
  cert: fs.readFileSync(path.join(__dirname, "server.cert")),
};
https.createServer(options, app)
  .listen(3000, function (req, res) {
    console.log("Server started at port 3000")
  })
