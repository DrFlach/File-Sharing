// Обработчик закрытия модального окна
document.addEventListener('click', function(event) {
    const modal = document.getElementById('modal');
    const modalContent = document.querySelector('.modal-content');
    if (modal && event.target === modal && !modalContent.contains(event.target)) {
        closeModal();
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const fileUploadWrapper = document.querySelector('.file-upload-wrapper');
    const userInfo = document.getElementById('userInfo');

    if (userInfo) {
        const userRole = userInfo.getAttribute('data-user-role');
        if (userRole !== 'ROLE_ADMIN' && userRole !== 'ROLE_STUDENT') {
            fileUploadWrapper.classList.add('hidden');
        }
    }

    const fileInput = document.getElementById('file');
    fileInput.addEventListener('change', function() {
        const fileName = this.files[0] ? this.files[0].name : '';
        const selectedFileSpan = document.querySelector('.selected-file');
        selectedFileSpan.textContent = fileName;
    });
});

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
        alert("Error: University ID not found");
        return;
    }

    formData.append('universityId', universityId);


    // Проверка, есть ли файлы и не пустые ли они
    var fileInput = form.querySelector('input[type="file"]');
    var files = fileInput.files;
    if (files.length === 0) {
        alert("Please select a file to upload.");
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
        alert("The file cannot be empty.");
        return;
    }

    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Authorization is required.");
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
                    throw new Error("A file with this name already exists in this semester.");
                }
                throw new Error(errorText || 'File upload error.');
            }
            return response.text();
        })
        .then(result => {
            alert("The file has been successfully uploaded!");
            fetchFiles();
            form.reset();
        })
        .catch(error => {
            console.error("File upload error.:", error);
            alert(error.message || "File upload error.");
        });
}

function openModal(semester) {
    // Проверка наличия всех необходимых элементов
    const userInfo = document.getElementById('userInfo');
    const modal = document.getElementById('modal');
    const semesterNumber = document.getElementById('semester-number');
    const hiddenSemester = document.getElementById('hiddenSemester');
    const addFileButton = document.getElementById('addFileButton');

    // Логируем для отладки
    console.log('Found elements:', {
        userInfo: !!userInfo,
        modal: !!modal,
        semesterNumber: !!semesterNumber,
        hiddenSemester: !!hiddenSemester,
        addFileButton: !!addFileButton
    });


    if (userInfo) {
        const userRole = userInfo.getAttribute('data-user-role');
        console.log('User role:', userRole);

        // Проверяем наличие кнопки добавления файла
        if (addFileButton) {
            if (userRole === 'ROLE_ADMIN' || userRole === 'ROLE_STUDENT') {
                console.log('Showing add file button for role:', userRole);
                addFileButton.style.display = 'inline-block';
            } else {
                console.log('Hiding add file button. Current role:', userRole);
                addFileButton.style.display = 'none';
            }
        }
    }

    // Устанавливаем номер семестра если элементы существуют
    if (semesterNumber) {
        semesterNumber.innerText = semester;
    }
    if (hiddenSemester) {
        hiddenSemester.value = semester;
    }

    // Открываем модальное окно если оно существует
    if (modal) {
        modal.classList.add('active');
    }

    fetchFiles(); // Загрузить список файлов при открытии модального окна
}

function closeModal() {
    document.getElementById('modal').classList.remove('active');
}

// Функция для получения списка файлов с сервера
function fetchFiles() {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        console.error("Authorization token not found.");
        return;
    }

    // Получаем universityId из data-container
    const dataContainer = document.getElementById('data-container');
    if (!dataContainer) {
        console.error("data-container not found");
        return;
    }
    const universityId = dataContainer.dataset.universityId;
    console.log('Data container:', dataContainer);
    console.log('University ID:', universityId);

    if (!universityId) {
        console.error("University ID not found");
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
            console.error("File list upload error:", error);
        });
}

function downloadFile(fileName) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Authorization is required");
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
            console.error("File download error.:", error);
            alert("File download error.");
        });
}

function deleteFile(fileName) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Authorization is required");
        return;
    }

    if (confirm('Are you sure you want to delete the file?')) {
        const fileUrl = "/files/delete?fileName=" + encodeURIComponent(fileName);

        fetch(fileUrl, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => {
                if (response.ok) {
                    alert("The file has been successfully deleted.");
                    fetchFiles(); // Обновляем список файлов
                } else {
                    throw new Error('Failed to delete file');
                }
            })
            .catch(error => {
                console.error("File deletion error:", error);
                alert("File deletion error:");
            });
    }
}

// Функция для обновления списка файлов в модальном окне
function updateFileList(groupedFiles) {
    const semesterFileCounts = {};
    const fileListContainer = document.getElementById('file-list-container');
    const currentSemester = parseInt(document.getElementById('hiddenSemester').value, 10);
    const userInfo = document.getElementById('userInfo');
    const isAdmin = userInfo ? userInfo.getAttribute('data-is-admin') === 'true' : false;

    fileListContainer.innerHTML = '';

    // Очистка списков файлов для каждого семестра
    for (let i = 1; i <= 8; i++) {
        const semesterContainer = document.querySelector(`#semester-${i} .file-list`);
        if (semesterContainer) {
            semesterContainer.innerHTML = '';
        }
    }

    function translateFaculty(faculty) {
        const translations = {
            'Informatyka': 'Computer Science',
            'Zarządzanie': 'Management',
            'Ekonomia': 'Economics',
            'Prawo': 'Law'
        };
        return translations[faculty] || faculty;
    }

    for (const group in groupedFiles) {
        if (groupedFiles.hasOwnProperty(group)) {
            const [faculty, semesterPart] = group.split(' - Семестр ');
            const semesterNumber = parseInt(semesterPart, 10);

            if (semesterNumber === currentSemester) {
                if (!semesterFileCounts[semesterNumber]) {
                    semesterFileCounts[semesterNumber] = 0;
                }

                const groupHeader = document.createElement('h4');
                groupHeader.textContent = `${faculty} - Семестр ${semesterNumber}`;
                const translatedFaculty = translateFaculty(faculty);
                groupHeader.textContent = `${translatedFaculty} - Semester ${semesterNumber}`;
                fileListContainer.appendChild(groupHeader);

                const fileList = groupedFiles[group];
                if (Array.isArray(fileList)) {
                    semesterFileCounts[semesterNumber] += fileList.length;
                    const ul = document.createElement('ul');

                    fileList.forEach(function(fileName) {
                        if (fileName && fileName.trim() !== "") {
                            const listItem = document.createElement('li');
                            listItem.style.display = 'flex';
                            listItem.style.alignItems = 'center';
                            listItem.style.justifyContent = 'space-between';

                            // Название файла
                            const fileNameSpan = document.createElement('span');
                            fileNameSpan.textContent = fileName;
                            fileNameSpan.style.marginRight = '10px';

                            // Контейнер для кнопок
                            const buttonsContainer = document.createElement('div');
                            buttonsContainer.style.display = 'flex';
                            buttonsContainer.style.gap = '10px';

                            // Кнопка для скачивания
                            const downloadButton = document.createElement('img');
                            downloadButton.src = '/images/download.png';
                            downloadButton.alt = 'Download';
                            downloadButton.style.cursor = 'pointer';
                            downloadButton.style.width = '20px';
                            downloadButton.style.verticalAlign = 'middle';
                            downloadButton.onclick = (e) => {
                                e.stopPropagation();
                                downloadFile(fileName);
                            };
                            buttonsContainer.appendChild(downloadButton);

                            // Кнопка удаления для админа
                            if (isAdmin) {
                                const deleteButton = document.createElement('img');
                                deleteButton.src = '/images/delete.png';
                                deleteButton.alt = 'Delete';
                                deleteButton.style.cursor = 'pointer';
                                deleteButton.style.width = '20px';
                                deleteButton.style.verticalAlign = 'middle';
                                deleteButton.onclick = (e) => {
                                    e.stopPropagation();
                                    deleteFile(fileName);
                                };
                                buttonsContainer.appendChild(deleteButton);
                            }

                            // Добавляем элементы в список
                            listItem.appendChild(fileNameSpan);
                            listItem.appendChild(buttonsContainer);

                            // Обработчик для открытия файла
                            listItem.addEventListener('click', function() {
                                openFileInNewTab(fileName);
                            });

                            ul.appendChild(listItem);
                        }
                    });
                    fileListContainer.appendChild(ul);
                } else {
                    console.error(`Incorrect data format for the group. ${group}:`, fileList);
                }
            }
        }
    }
}

//открытие файла
function openFileInNewTab(fileName) {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
        alert("Authorization is required.");
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
            console.error("File opening error:", error);
            alert("File opening error.");
        });
}