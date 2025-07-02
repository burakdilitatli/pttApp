const WebSocket = require('ws')
const wss = new WebSocket.Server({ host: '0.0.0.0', port: 8080 })

wss.on('connection', ws => {
  console.log('Yeni bağlantı')
  ws.on('message', msg => ws.send(msg))
})
