async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        throw new Error('No authentication token found');
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };

    return fetch(url, { ...options, headers });
}

function submitForm() {
    var form = document.getElementById('uploadForm');
    var formData = new FormData(form);

    const dataContainer = document.getElementById('data-container');
    const universityId = dataContainer.dataset.universityId;

    console.log('Submitting form with university ID:', universityId);

    if (!universityId) {
        alert("Ошибка: ID университета не найден");
        return;
    }

    formData.append('universityId', universityId);


    // Проверка, есть ли файлы и не пустые ли они
    var fileInput = form.querySelector('input[type="file"]');
    var files = fileInput.files;
    if (files.length === 0) {
        alert("Пожалуйста, выберите файл для загрузки.");
        return;
    }

    var validFile = false;
    for (var i = 0; i < files.length; i++) {
        if (files[i].size > 0 && files[i].name.trim() !== "") {
            validFile = true;
            break;
        }
    }

    if (!validFile) {
        alert("Файл не может быть пустым.");
        return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Необходима авторизация");
        return;
    }

    fetch(form.action, {
        method: 'POST',
        body: formData,
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(async response => {
            if (!response.ok) {
                const errorText = await response.text();
                if (response.status === 400 && errorText.includes("already exists")) {
                    throw new Error("Файл с таким именем уже существует в этом семестре");
                }
                throw new Error(errorText || 'Ошибка загрузки файла');
            }
            return response.text();
        })
        .then(result => {
            alert("Файл успешно загружен!");
            fetchFiles();
            form.reset();
        })
        .catch(error => {
            console.error("Ошибка загрузки файла:", error);
            alert(error.message || "Ошибка загрузки файла");
        });
}

function openModal(semester) {
    try {
        console.log('Opening modal for semester:', semester);
        const modal = document.getElementById('modal');
        const semesterNumber = document.getElementById('semester-number');
        const hiddenSemester = document.getElementById('hiddenSemester');

        console.log('Modal element:', modal);
        console.log('Semester number element:', semesterNumber);
        console.log('Hidden semester element:', hiddenSemester);

        if (!modal || !semesterNumber || !hiddenSemester) {
            throw new Error('Required modal elements not found');
        }

        semesterNumber.innerText = semester;
        hiddenSemester.value = semester;

        modal.style.display = 'flex'; // Добавим явное указание стиля
        modal.classList.add('active');

        console.log('Modal classes after opening:', modal.classList);

        fetchFiles();
    } catch (error) {
        console.error('Error opening modal:', error);
    }
}

function closeModal() {
    try {
        const modal = document.getElementById('modal');
        if (!modal) {
            throw new Error('Modal element not found');
        }
        modal.classList.remove('active');
        modal.style.display = 'none';
        console.log('Modal closed successfully');
    } catch (error) {
        console.error('Error closing modal:', error);
    }
}

// Функция для получения списка файлов с сервера
function fetchFiles() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error("Токен авторизации не найден");
        return;
    }

    // Получаем universityId из data-container
    const dataContainer = document.getElementById('data-container');
    if (!dataContainer) {
        console.error("data-container не найден");
        return;
    }
    const universityId = dataContainer.dataset.universityId;
    console.log('Data container:', dataContainer);
    console.log('University ID:', universityId);

    if (!universityId) {
        console.error("University ID не найден");
        return;
    }

    // Формируем URL с помощью URLSearchParams
    const url = new URL('/files/grouped', window.location.origin);
    url.searchParams.append('universityId', universityId);

    console.log('Fetching from URL:', url.toString());

    fetch(url, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            throw new Error('Failed to fetch files');
        })
        .then(files => {
            updateFileList(files);
        })
        .catch(error => {
            console.error("Ошибка загрузки списка файлов:", error);
        });
}

function downloadFile(fileName) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Необходима авторизация");
        return;
    }

    const fileUrl = "/files/download?fileName=" + encodeURIComponent(fileName);

    fetch(fileUrl, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.blob())
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        })
        .catch(error => {
            console.error("Ошибка скачивания файла:", error);
            alert("Ошибка скачивания файла");
        });
}

// Функция для обновления списка файлов в модальном окне
function updateFileList(groupedFiles) {
    const semesterFileCounts = {};  // Для подсчета файлов по семестрам
    const fileListContainer = document.getElementById('file-list-container');
    const currentSemester = parseInt(document.getElementById('hiddenSemester').value, 10);
    fileListContainer.innerHTML = ''; // Очистить текущий список

    // Очистка списков файлов для каждого семестра
    for (let i = 1; i <= 8; i++) {
        const semesterContainer = document.querySelector(`#semester-${i} .file-list`);
        if (semesterContainer) {
            semesterContainer.innerHTML = ''; // Очистка списка файлов в блоках семестров
        }
    }

    // Перебираем группы (факультеты - семестр ) и создаём списки
    for (const group in groupedFiles) {
        if (groupedFiles.hasOwnProperty(group)) {
            // Разделяем название факультета и номер семестра
            const [faculty, semesterPart] = group.split(' - Семестр ');
            const semesterNumber = parseInt(semesterPart, 10);

            // Если это текущий выбранный семестр, добавляем файлы в модальное окно
            if (semesterNumber === currentSemester) {
                if (!semesterFileCounts[semesterNumber]) {
                    semesterFileCounts[semesterNumber] = 0;
                }

                const groupHeader = document.createElement('h4');
                groupHeader.textContent = `${faculty} - Семестр ${semesterNumber}`;
                fileListContainer.appendChild(groupHeader);

                const fileList = groupedFiles[group];
                if (Array.isArray(fileList)) {
                    semesterFileCounts[semesterNumber] += fileList.length;  // Подсчитываем файлы для этого семестра
                    const ul = document.createElement('ul');

                    fileList.forEach(function (fileName) {
                        if (fileName && fileName.trim() !== "") {  // Проверка на пустые имена файлов
                            const listItem = document.createElement('li');
                            listItem.style.display = 'flex';
                            listItem.style.alignItems = 'center';

                            // Название файла
                            const fileNameSpan = document.createElement('span');
                            fileNameSpan.textContent = fileName;
                            fileNameSpan.style.marginRight = '10px';

                            // Кнопка для скачивания
                            const downloadButton = document.createElement('img');
                            downloadButton.src = '/images/download.png';
                            downloadButton.alt = 'Download';
                            downloadButton.style.cursor = 'pointer';
                            downloadButton.style.width = '20px';
                            downloadButton.style.verticalAlign = 'middle';

                            // Обработчик для скачивания
                            downloadButton.addEventListener('click', function (event) {
                                event.stopPropagation(); // Предотвращаем срабатывание клика на элемент списка
                                downloadFile(fileName);
                            });

                            // Добавляем элементы в список
                            listItem.appendChild(fileNameSpan);
                            listItem.appendChild(downloadButton);

                            // Обработчик для открытия файла
                            listItem.addEventListener('click', function () {
                                openFileInNewTab(fileName);
                            });

                            ul.appendChild(listItem);
                        }
                    });
                    fileListContainer.appendChild(ul);
                } else {
                    console.error(`Ошибочный формат данных для группы ${group}:`, fileList);
                }
            }
        }
    }
}


//открытие файла
function openFileInNewTab(fileName) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Необходима авторизация");
        return;
    }

    const fileUrl = "/files/content?fileName=" + encodeURIComponent(fileName);

    fetch(fileUrl, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => {
            if (response.ok) {
                window.open(fileUrl, '_blank');
            } else {
                throw new Error('Failed to open file');
            }
        })
        .catch(error => {
            console.error("Ошибка открытия файла:", error);
            alert("Ошибка открытия файла");
        });
}

window.onclick = function(event) {
    const modal = document.getElementById('modal');
    if (event.target === modal) {
        closeModal();
    }
}