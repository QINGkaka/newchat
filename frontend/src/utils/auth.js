const connectSocket = () => {
    const token = localStorage.getItem('token');
    if (token) {
        socket.auth = { token };
        socket.connect();
    }
};