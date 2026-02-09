from flask import Flask, jsonify
from datetime import datetime

app = Flask(__name__)

@app.route('/')
def home():
    return '''
    <html>
        <head><title>Flask Demo</title></head>
        <body style="font-family: Arial; text-align: center; padding: 50px;">
            <h1>🐍 Hello from Flask on ECS!</h1>
            <p>This Python app is running on AWS ECS (EC2 launch type)</p>
            <p>Container Port: 5000</p>
            <p>Timestamp: {}</p>
        </body>
    </html>
    '''.format(datetime.utcnow().isoformat() + 'Z')

@app.route('/health')
def health():
    return jsonify({
        'status': 'healthy',
        'runtime': 'python-flask',
        'timestamp': datetime.utcnow().isoformat() + 'Z'
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
