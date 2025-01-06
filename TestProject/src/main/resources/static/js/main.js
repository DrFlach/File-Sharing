function filterUniversities() {
    const input = document.getElementById('searchInput');
    const filter = input.value.toLowerCase();
    const list = document.getElementById('universityList');
    const items = list.getElementsByClassName('university-item');

    for (let i = 0; i < items.length; i++) {
        const name = items[i].getElementsByClassName('university-name')[0].innerText.toLowerCase();
        items[i].style.display = name.includes(filter) ? "" : "none";
    }
}

async function fetchWithToken(url, options = {}) {
    const token = localStorage.getItem('jwtToken');

    const headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`
    };

    try {
        const response = await fetch(url, { ...options, headers });

        if (!response.ok) {
            if (response.status === 401) {
                // Token might be expired, redirect to login
                window.location.href = '/login';
                throw new Error('Unauthorized');
            }
            throw new Error('Request failed');
        }

        return response.json();
    } catch (error) {
        console.error('Fetch error:', error);
        throw error;
    }
}

async function loadUserInfo() {
    const token = localStorage.getItem('jwtToken');
    console.log("Using token:", token);

    if (!token) {
        console.warn("No JWT token found in localStorage.");
        document.getElementById('user-info').textContent = "You are not logged in.";
        window.location.href = '/login';
        return;
    }

    try {
        const response = await fetch('/api/userinfo', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        console.log("Response status:", response.status);

        if (response.ok) {
            const userInfo = await response.json(); // Читаем response.json() только один раз
            console.log("User info:", userInfo);

            const userInfoElement = document.getElementById('user-info');
            if (userInfoElement) {
                userInfoElement.textContent = `Logged in as: ${userInfo.username} (${userInfo.roles.join(', ')})`;
            }
        } else {
            const errorText = await response.text();
            console.error("Error response:", errorText);
            document.getElementById('user-info').textContent =
                `Error loading user info: ${response.status}`;

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('jwtToken');
                window.location.href = '/login';
            }
        }
    } catch (error) {
        console.error("Error fetching user info:", error);
        document.getElementById('user-info').textContent =
            `Error: ${error.message}`;
    }
}

// Вызываем функцию при загрузке страницы
document.addEventListener('DOMContentLoaded', loadUserInfo);