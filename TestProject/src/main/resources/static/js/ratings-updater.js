let stompClient = null;

// Функции для фильтрации университетов
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

// Общая функция для fetch запросов с токеном
async function fetchWithToken(url, options = {}) {
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
}

function setupWebSocket() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error('No JWT token found');
        return;
    }

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    const connectHeaders = {
        'Authorization': `Bearer ${token}`
    };

    console.log('Connecting to WebSocket...');

    stompClient.connect(connectHeaders,
        function(frame) {
            console.log('Connected to WebSocket');

            // Load initial ratings first
            loadInitialRatings().then(() => {
                // Then subscribe to updates
                stompClient.subscribe('/topic/ratings', function(message) {
                    try {
                        const ratingData = JSON.parse(message.body);
                        console.log('Received rating update:', ratingData);
                        if (ratingData && ratingData.universityId && ratingData.averageRating != null) {
                            updateRating(ratingData);
                        } else {
                            console.error('Invalid rating data received:', ratingData);
                        }
                    } catch (error) {
                        console.error('Error processing rating update:', error);
                    }
                });
            });
        },
        function(error) {
            console.error('WebSocket connection error:', error);
            setTimeout(() => setupWebSocket(), 5000);
        }
    );
}

function updateRating(data) {
    const universityElement = document.querySelector(`.university-item[data-university-id="${data.universityId}"]`);
    if (!universityElement) {
        console.error('University element not found for ID:', data.universityId);
        return;
    }

    const ratingElement = universityElement.querySelector('.university-rating');
    if (!ratingElement) {
        console.error('Rating element not found');
        return;
    }

    const newRating = Number(data.averageRating).toFixed(2);
    console.log(`Updating rating for university ${data.universityId} from ${ratingElement.textContent} to ${newRating}`);

    ratingElement.style.transition = 'opacity 0.3s ease-in-out';
    ratingElement.style.opacity = '0';

    setTimeout(() => {
        ratingElement.textContent = newRating;
        ratingElement.style.opacity = '1';
    }, 300);
}

async function loadInitialRatings() {
    try {
        const response = await fetchWithToken('/uni/ratings');
        const universities = await response.json();
        console.log('Loaded initial ratings:', universities);

        universities.forEach(updateRating);
    } catch (error) {
        console.error('Error loading initial ratings:', error);
    }
}

async function loadUserInfo() {
    try {
        const response = await fetchWithToken('/api/userinfo');
        const userInfo = await response.json();
        console.log("User info loaded:", userInfo);

        const userInfoElement = document.getElementById('user-info');
        if (userInfoElement) {
            userInfoElement.textContent = `Logged in as: ${userInfo.username} (${userInfo.roles.join(', ')})`;
        }
    } catch (error) {
        console.error("Error fetching user info:", error);
        const userInfoElement = document.getElementById('user-info');
        if (userInfoElement) {
            userInfoElement.textContent = `Error: ${error.message}`;
        }
    }
}

// Единственный обработчик DOMContentLoaded
document.addEventListener('DOMContentLoaded', async function() {
    console.log('Page loaded, initializing...');

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error('No JWT token found');
        window.location.href = '/login';
        return;
    }

    try {
        // Сначала загружаем информацию о пользователе
        await loadUserInfo();

        // Затем устанавливаем WebSocket соединение
        setupWebSocket();

        // Загружаем начальные рейтинги
        await loadInitialRatings();

        // Запускаем периодическое обновление рейтингов
        setInterval(loadInitialRatings, 30000);

        // Логируем информацию об университетах на странице
        const universities = document.querySelectorAll('.university-item');
        universities.forEach(uni => {
            console.log(`University: ${uni.querySelector('.university-name').textContent}, ID: ${uni.dataset.universityId}`);
        });
    } catch (error) {
        console.error('Initialization error:', error);
    }
});

document.addEventListener('DOMContentLoaded', function() {
    // Обработка клика вне выпадающих меню для их закрытия
    document.addEventListener('click', function(event) {
        if (!event.target.closest('.dropdown') && !event.target.closest('.profile-menu')) {
            const dropdowns = document.querySelectorAll('.dropdown-content, .profile-dropdown');
            dropdowns.forEach(dropdown => {
                dropdown.style.display = 'none';
            });
        }
    });
});