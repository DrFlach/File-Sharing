// Получаем pageId из скрытого элемента
const pageData = document.getElementById('pageData');
const userInfo = document.getElementById('userInfo');
const isAdmin = userInfo ? userInfo.getAttribute('data-is-admin') === 'true' : false;
let pageId = pageData ? pageData.getAttribute('data-page-id') : null;
let stompClient = null;
let isConnected = false;
let reconnectAttempts = 0;

function checkAuthentication() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error('No JWT token found');
        // Можно добавить редирект на страницу логина или показать сообщение пользователю
        return false;
    }
    return true;
}

function addComment(comment) {
    const commentsList = document.getElementById('commentsList');
    if (!commentsList) {
        console.error('Element commentsList not found');
        return;
    }

    const commentElement = document.createElement('div');
    commentElement.className = 'comment';
    commentElement.setAttribute('data-comment-id', comment.id);

    let deleteButton = '';
    if (isAdmin) {
        deleteButton = `
            <div class="comment-actions">
                <button class="delete-btn" onclick="deleteComment(${comment.id})">
                    <svg class="delete-icon" viewBox="0 0 24 24">
                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                    </svg>
                </button>
            </div>
        `;
    }
    commentElement.innerHTML = `
        <div class="comment-header">
            <div class="comment-info">
                <span class="username">${comment.username}</span>
                <span class="date">${new Date(comment.createdAt).toLocaleString()}</span>
            </div>
            ${deleteButton}
        </div>
        <div class="comment-content">${comment.content}</div>
    `;
    commentsList.insertBefore(commentElement, commentsList.firstChild);
}

async function deleteComment(commentId) {
    if (!checkAuthentication()) {
        return;
    }

    const commentElement = document.querySelector(`[data-comment-id="${commentId}"]`);
    if (!commentElement) return;

    try {
        const token = localStorage.getItem('jwtToken');
        commentElement.classList.add('deleting'); // Добавляем класс для анимации

        const response = await fetch(`/api/comments/${commentId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // Ждем окончания анимации перед удалением элемента
        setTimeout(() => {
            commentElement.remove();
        }, 300); // Время должно совпадать с длительностью анимации в CSS

    } catch (error) {
        console.error('Error deleting comment:', error);
        commentElement.classList.remove('deleting'); // Убираем класс анимации в случае ошибки
    }
}

async function loadExistingComments() {
    try {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            console.error('No JWT token found');
            return;
        }

        const response = await fetch(`/api/comments/${pageId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const comments = await response.json();
        if (Array.isArray(comments)) {
            comments.forEach(addComment);
        } else {
            console.error('Received invalid comments data:', comments);
        }
    } catch (error) {
        console.error('Error loading comments:', error);
    }
}

function setupHeartbeat() {
    if (stompClient && stompClient.heartbeat) {
        stompClient.heartbeat.outgoing = 10000;
        stompClient.heartbeat.incoming = 10000;
    }
}

function sendComment() {
    if (!stompClient || !isConnected) {
        console.error('WebSocket not connected');
        connect(); // Попытка переподключения
        return;
    }

    const content = document.getElementById('commentContent');
    if (!content || content.value.trim() === '') {
        return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error('No JWT token found');
        return;
    }

    const headers = {
        'Authorization': `Bearer ${token}`
    };

    const message = {
        content: content.value.trim()
    };

    try {
        stompClient.send(
            `/app/comment/${pageId}`,
            headers,
            JSON.stringify(message)
        );
        content.value = '';
    } catch (error) {
        console.error('Error sending comment:', error);
        if (!isConnected) {
            connect();
        }
    }
}

function connect() {
    if (!pageId) {
        console.error('No pageId found');
        return;
    }

    const token = localStorage.getItem('jwtToken');
    console.log('Using token:', token ? 'exists' : 'not found');
    if (!token) {
        console.error('No JWT token found');
        return;
    }

    try {
        if (stompClient) {
            stompClient.disconnect();
        }

        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Отключаем отладочные сообщения

        const connectHeaders = {
            'Authorization': `Bearer ${token}`
        };

        console.log('Connecting with headers:', connectHeaders);

        stompClient.connect(
            connectHeaders,
            function(frame) {
                console.log('Connected to WebSocket');
                isConnected = true;
                reconnectAttempts = 0;

                stompClient.subscribe(
                    `/topic/comments/${pageId}`,
                    function(response) {
                        try {
                            const comment = JSON.parse(response.body);
                            addComment(comment);
                        } catch (error) {
                            console.error('Error processing comment:', error);
                        }
                    },
                    connectHeaders
                );
            },
            function(error) {
                console.error('STOMP error:', error);
                isConnected = false;
                setTimeout(connect, Math.min(1000 * Math.pow(2, reconnectAttempts++), 30000));
            }
        );
    } catch (error) {
        console.error('Error in connect:', error);
        setTimeout(connect, 5000);
    }
}

function handleNewComment(message) {
    try {
        const comment = JSON.parse(message.body);
        addComment(comment);
    } catch (error) {
        console.error('Error processing comment:', error);
    }
}

function disconnect() {
    if (stompClient) {
        try {
            stompClient.disconnect();
            isConnected = false;
            console.log('Disconnected from WebSocket');
        } catch (error) {
            console.error('Error during disconnect:', error);
        }
    }
}

function initialize() {
    if (!pageId) {
        console.error('PageId is not set');
        return;
    }

    if (!checkAuthentication()) {
        return;
    }

    loadExistingComments();
    connect();

    const sendButton = document.getElementById('sendCommentBtn');
    if (sendButton) {
        sendButton.addEventListener('click', sendComment);
    }

    const commentInput = document.getElementById('commentContent');
    if (commentInput) {
        commentInput.addEventListener('keypress', function(event) {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendComment();
            }
        });
    }

}

// Обработка закрытия окна
window.addEventListener('beforeunload', disconnect);

// Добавляем обработчик для переподключения при потере соединения
window.addEventListener('online', function() {
    if (!isConnected) {
        console.log('Network connection restored. Attempting to reconnect...');
        reconnectAttempts = 0; // Сбрасываем счетчик попыток
        connect();
    }
});

// Инициализация при загрузке DOM
document.addEventListener('DOMContentLoaded', initialize);