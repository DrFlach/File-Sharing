// comments.js
import { Utils } from './utility.js';

class CommentsSystem {
    constructor() {
        this.pageData = document.getElementById('pageData');
        this.userInfo = document.getElementById('userInfo');
        this.pageId = this.pageData ? this.pageData.getAttribute('data-page-id') : null;
        this.isAdmin = this.userInfo ? this.userInfo.getAttribute('data-is-admin') === 'true' : false;
        this.stompClient = null;
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.init();
    }

    init() {
        if (!this.pageId) {
            console.error('PageId is not set');
            return;
        }

        if (!Utils.checkAuthentication()) {
            return;
        }

        this.loadExistingComments();
        this.connect();
        this.setupEventListeners();
    }

    setupEventListeners() {
        const sendButton = document.getElementById('sendCommentBtn');
        if (sendButton) {
            sendButton.addEventListener('click', () => this.sendComment());
        }

        const commentInput = document.getElementById('commentContent');
        if (commentInput) {
            commentInput.addEventListener('keypress', (event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault();
                    this.sendComment();
                }
            });

            // Автоматическое расширение текстового поля
            commentInput.addEventListener('input', () => {
                commentInput.style.height = 'auto';
                commentInput.style.height = commentInput.scrollHeight + 'px';
            });
        }
    }

    setupHeartbeat() {
        if (this.stompClient && this.stompClient.heartbeat) {
            this.stompClient.heartbeat.outgoing = 10000;
            this.stompClient.heartbeat.incoming = 10000;
        }
    }

    async loadExistingComments() {
        try {
            const response = await Utils.fetchWithAuth(`/api/comments/${this.pageId}`);
            const comments = await response.json();
            if (Array.isArray(comments)) {
                comments.forEach(comment => this.addComment(comment));
            }
        } catch (error) {
            console.error('Error loading comments:', error);
            Utils.showNotification('Error loading comments', 'error');
        }
    }

    connect() {
        if (!this.pageId) {
            console.error('No pageId found');
            return;
        }

        try {
            if (this.stompClient) {
                this.stompClient.disconnect();
            }

            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = null; // Отключаем отладочные сообщения

            const connectHeaders = {
                'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
            };

            this.stompClient.connect(
                connectHeaders,
                frame => {
                    console.log('Connected to WebSocket');
                    this.isConnected = true;
                    this.reconnectAttempts = 0;

                    this.stompClient.subscribe(
                        `/topic/comments/${this.pageId}`,
                        response => {
                            try {
                                const comment = JSON.parse(response.body);
                                this.addComment(comment);
                            } catch (error) {
                                console.error('Error processing comment:', error);
                            }
                        },
                        connectHeaders
                    );
                },
                error => {
                    console.error('STOMP error:', error);
                    this.isConnected = false;
                    setTimeout(() => this.connect(),
                        Math.min(1000 * Math.pow(2, this.reconnectAttempts++), 30000));
                }
            );
        } catch (error) {
            console.error('Error in connect:', error);
            setTimeout(() => this.connect(), 5000);
        }
    }

    sendComment() {
        if (!this.stompClient || !this.isConnected) {
            console.error('WebSocket not connected');
            this.connect();
            return;
        }

        const content = document.getElementById('commentContent');
        if (!content || content.value.trim() === '') {
            return;
        }

        const message = {
            content: content.value.trim()
        };

        try {
            this.stompClient.send(
                `/app/comment/${this.pageId}`,
                { 'Authorization': `Bearer ${localStorage.getItem('jwtToken')}` },
                JSON.stringify(message)
            );
            content.value = '';
            content.style.height = 'auto'; // Сброс высоты текстового поля
        } catch (error) {
            console.error('Error sending comment:', error);
            if (!this.isConnected) {
                this.connect();
            }
        }
    }

    addComment(comment) {
        const commentsList = document.getElementById('commentsList');
        if (!commentsList) return;

        const commentElement = document.createElement('div');
        commentElement.className = 'comment';
        commentElement.setAttribute('data-comment-id', comment.id);

        let deleteButton = '';
        if (this.isAdmin) {
            deleteButton = `
                <div class="comment-actions">
                    <button class="delete-btn" onclick="window.commentsSystem.deleteComment(${comment.id})">
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

        // Анимация появления
        commentElement.style.opacity = '0';
        commentElement.style.transform = 'translateY(-10px)';
        commentsList.insertBefore(commentElement, commentsList.firstChild);

        requestAnimationFrame(() => {
            commentElement.style.opacity = '1';
            commentElement.style.transform = 'translateY(0)';
        });
    }

    async deleteComment(commentId) {
        if (!Utils.checkAuthentication()) return;

        const commentElement = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!commentElement) return;

        try {
            await Utils.fetchWithAuth(`/api/comments/${commentId}`, { method: 'DELETE' });

            // Анимация удаления
            commentElement.style.transform = 'translateX(20px)';
            commentElement.style.opacity = '0';

            setTimeout(() => {
                commentElement.remove();
            }, 300);

            Utils.showNotification('Comment deleted successfully');
        } catch (error) {
            Utils.showNotification('Failed to delete comment', 'error');
        }
    }

    disconnect() {
        if (this.stompClient) {
            try {
                this.stompClient.disconnect();
                this.isConnected = false;
                console.log('Disconnected from WebSocket');
            } catch (error) {
                console.error('Error during disconnect:', error);
            }
        }
    }
}

// Создаем глобальный экземпляр для доступа к deleteComment из HTML
window.commentsSystem = new CommentsSystem();

// Обработка закрытия окна
window.addEventListener('beforeunload', () => {
    window.commentsSystem.disconnect();
});

// Переподключение при восстановлении соединения
window.addEventListener('online', () => {
    if (!window.commentsSystem.isConnected) {
        console.log('Network connection restored. Attempting to reconnect...');
        window.commentsSystem.reconnectAttempts = 0;
        window.commentsSystem.connect();
    }
});

export default window.commentsSystem;