const api = axios.create({
    baseURL: 'http://localhost:19096/api',
    headers: {
        'Content-Type': 'application/json'
    }
});