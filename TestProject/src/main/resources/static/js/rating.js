// Общие утилиты
function getDataFromContainer() {
    const dataContainer = document.getElementById('data-container');
    if (!dataContainer) {
        console.error("Element with id 'data-container' is not found.");
        return null;
    }

    const universityId = dataContainer.dataset.universityId;
    const userId = dataContainer.dataset.userId;

    if (!universityId || !userId) {
        console.error("Missing required data: universityId or userId");
        document.getElementById('error-message').textContent =
            "Error: Missing required data";
        return null;
    }

    return {
        universityId: universityId,
        userId: userId
    };
}

async function fetchWithAuth(url, options = {}) {
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

// Функции для работы с рейтингами
async function getUserRating() {
    const data = getDataFromContainer();
    if (!data) return null;

    try {
        const response = await fetchWithAuth(
            `/uni/${data.universityId}/ratings/user?userId=${data.userId}`
        );
        return await response.json();
    } catch (error) {
        console.error("Error getting user rating:", error);
        return null;
    }
}

async function submitRating(rating) {
    const data = getDataFromContainer();
    if (!data) {
        alert("Could not get university or user data");
        return;
    }

    try {
        const response = await fetchWithAuth(`/uni/${data.universityId}/ratings/add`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                rating: parseFloat(rating),
                universityId: parseInt(data.universityId),
                userId: parseInt(data.userId)
            })
        });

        const result = await response.json();

        if (response.ok) {
            alert("Thank you for your rating!");
            const stars = document.querySelectorAll('.star');

            stars.forEach((star) => {
                const starRating = parseInt(star.dataset.rating);
                if (starRating <= rating) {
                    star.classList.add('active');
                    star.querySelector('i').style.color = '#ffd700';
                } else {
                    star.classList.remove('active');
                    star.querySelector('i').style.color = '#ddd';
                }
            });

            document.querySelector('.remove-rating-btn').style.display = 'block';

            await updateAverageRating();
        }
    } catch (error) {
        console.error("Error submitting rating:", error);
        alert(error.message.includes('Authentication failed')
            ? "Please log in to rate this university"
            : "Failed to submit rating");
    }
}

async function removeRating() {
    const data = getDataFromContainer();
    if (!data) return;

    try {
        const response = await fetchWithAuth(
            `/uni/${data.universityId}/ratings/remove?userId=${data.userId}`,
            { method: 'DELETE' }
        );

        if (response.ok) {
            alert("Your rating has been removed");
            const stars = document.querySelectorAll('.star');
            stars.forEach(star => {
                star.classList.remove('active');
                star.querySelector('i').style.color = '#ddd';
            });
            await updateAverageRating();
            await updateUI();
        }
    } catch (error) {
        console.error("Error removing rating:", error);
        alert("Failed to remove rating");
    }
}

async function updateAverageRating() {
    const data = getDataFromContainer();
    if (!data) return;

    try {
        const response = await fetchWithAuth(`/uni/${data.universityId}/ratings/average`);
        const statistics = await response.json();

        document.getElementById('average-rating').textContent =
            typeof statistics.averageRating === 'number' ?
                statistics.averageRating.toFixed(2) : 'No ratings yet';

        document.getElementById('vote-count').textContent = statistics.voteCount;
    } catch (error) {
        console.error("Error updating rating statistics:", error);
        document.getElementById('average-rating').textContent = 'Error loading rating';
        document.getElementById('vote-count').textContent = '-';
    }
}

// Функция для обновления отображения звезд
function updateStarsDisplay(stars, rating) {
    const starsArray = Array.from(stars);
    // starsArray.reverse(); // Разворачиваем массив, так как звезды идут справа налево
    starsArray.forEach(star => {
        const starRating = parseInt(star.dataset.rating);
        if (starRating <= rating) {
            star.querySelector('i').style.color = '#ffd700';
        } else {
            star.querySelector('i').style.color = '#ddd';
        }
    });
}

// Функция для сброса отображения звезд
function resetStarsDisplay(stars) {
    const starsArray = Array.from(stars);
    const activeStars = starsArray.filter(star => star.classList.contains('active'));
    const maxRating = activeStars.length > 0 ?
        Math.max(...activeStars.map(star => parseInt(star.dataset.rating))) : 0;

    starsArray.forEach(star => {
        const starRating = parseInt(star.dataset.rating);
        if (maxRating > 0) {
            // Если есть голос, подсвечиваем до выбранного рейтинга
            star.querySelector('i').style.color = starRating <= maxRating ? '#ffd700' : '#ddd';
        } else {
            // Если голоса нет, все звезды серые
            star.querySelector('i').style.color = '#ddd';
        }
    });
}

// UI элементы
function createRatingControls() {
    const ratingSection = document.createElement('div');
    ratingSection.className = 'rating-container';
    ratingSection.innerHTML = `
            <div id="rating-controls">
            <div class="stars">
                ${[1,2,3,4,5].map(num => `
                    <span class="star" data-rating="${num}">
                        <i class="fas fa-star" style="color: #ddd;"></i>
                    </span>
                `).join('')}
            </div>
            <p class="rating-label">Click on stars to rate</p>
        </div>
        <div id="current-rating-section" style="display: none;">
            <div class="current-user-rating">
                <p id="current-rating"></p>
                <button class="remove-rating-btn" onclick="removeRating()">
                    <i class="fas fa-trash"></i> Remove Rating
                </button>
            </div>
        </div>
    `;

    // Добавляем обработчики событий для звезд
    const stars = ratingSection.querySelectorAll('.star');
    stars.forEach(star => {
        star.addEventListener('click', () => {
            const rating = star.dataset.rating;
            submitRating(rating);
        });

        // Подсветка звезд при наведении
        star.addEventListener('mouseover', () => {
            const rating = star.dataset.rating;
            // Проверяем, есть ли уже активные звезды
            const hasActiveStars = Array.from(stars).some(s => s.classList.contains('active'));
            if (!hasActiveStars) {
                updateStarsDisplay(stars, rating);
            }
        });

        // Сброс подсветки при уходе курсора
        star.addEventListener('mouseout', () => {
            const hasActiveStars = Array.from(stars).some(s => s.classList.contains('active'));
            if (!hasActiveStars) {
                resetStarsDisplay(Array.from(stars));
            }
        });
    });

    document.body.appendChild(ratingSection);
}

async function updateUI() {
    const userRating = await getUserRating();
    const ratingControls = document.getElementById('rating-controls');
    const currentRatingSection = document.getElementById('current-rating-section');
    const stars = document.querySelectorAll('.star');
    const starsArray = Array.from(stars);
    const removeButton = document.querySelector('.remove-rating-btn');

    if (userRating && userRating.hasVoted) {
        stars.forEach((star) => {
            const starRating = parseInt(star.dataset.rating);
            if (starRating <= userRating.rating) {
                star.classList.add('active');
                star.querySelector('i').style.color = '#ffd700';
            } else {
                star.classList.remove('active');
                star.querySelector('i').style.color = '#ddd';
            }
        });

        removeButton.style.display = 'block';
    } else {
        stars.forEach(star => {
            star.classList.remove('active');
            star.querySelector('i').style.color = '#ddd';
        });
        removeButton.style.display = 'none';
    }

    await updateAverageRating();
}

document.addEventListener('DOMContentLoaded', async function () {
    const stars = document.querySelectorAll('#rating-controls .star');
    const starsContainer = document.querySelector('.stars');

    stars.forEach(star => {
        star.addEventListener('click', () => {
            const rating = star.dataset.rating;
            submitRating(rating);
        });

        star.addEventListener('mouseover', () => {
            const rating = star.dataset.rating;
            const hasActiveStars = Array.from(stars).some(s => s.classList.contains('active'));
            if (!hasActiveStars) {
                updateStarsDisplay(stars, rating);
            }
        });
    });

    starsContainer.addEventListener('mouseleave', () => {
        const currentStars = document.querySelectorAll('.star');
        resetStarsDisplay(Array.from(currentStars));
    });

    try {
        await updateUI();
    } catch (error) {
        console.error("Error during initialization:", error);
        if (error.message.includes('Authentication failed')) {
            window.location.href = '/login';
        }
    }
});