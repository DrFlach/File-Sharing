// utils.js
export const Utils = {
    async fetchWithAuth(url, options = {}) {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            throw new Error('No authentication token found');
        }

        const headers = {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
            ...options.headers
        };

        const response = await fetch(url, { ...options, headers });
        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('jwtToken');
                window.location.href = '/login';
                throw new Error('Authentication failed');
            }
            throw new Error(`Request failed with status ${response.status}`);
        }
        return response;
    },

    showNotification(message, type = 'success') {
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span>${message}</span>
                <button onclick="this.parentElement.parentElement.remove()">×</button>
            </div>
        `;
        document.body.appendChild(notification);

        requestAnimationFrame(() => {
            notification.style.transform = 'translateX(0)';
            notification.style.opacity = '1';
        });

        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            notification.style.opacity = '0';
            setTimeout(() => notification.remove(), 300);
        }, 5000);
    },

    getDataFromContainer() {
        const dataContainer = document.getElementById('data-container');
        if (!dataContainer) {
            console.error("Element with id 'data-container' is not found.");
            return null;
        }

        const universityId = dataContainer.dataset.universityId;
        const userId = dataContainer.dataset.userId;

        if (!universityId || !userId) {
            console.error("Missing required data: universityId or userId");
            return null;
        }

        return {
            universityId: universityId,
            userId: userId
        };
    },

    checkAuthentication() {
        const token = localStorage.getItem('jwtToken');
        if (!token) {
            console.error('No JWT token found');
            return false;
        }
        return true;
    }
};

// export default Utils;