const WebSocket = require('ws')
const wss = new WebSocket.Server({ port: 8080 })

wss.on('connection', ws => {
  console.log('Yeni bağlantı')
  ws.on('message', msg => {
    // Gelen ham PCM veriyi burada alırsın
    ws.send(msg)  // loopback test için aynı veriyi geri gönder
  })
})