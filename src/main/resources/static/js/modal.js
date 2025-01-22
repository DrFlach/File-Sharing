// modal.js
import { Utils } from './utility.js';

class ModalSystem {
    constructor() {
        this.modal = document.getElementById('modal');
        this.init();
        this.setupSemesterListeners();
    }

    setupSemesterListeners(){
        for (let i = 1; i <= 8; i++) {
            const semesterDiv = document.getElementById(`semester-${i}`);
            if (semesterDiv) {
                semesterDiv.addEventListener('click', () => this.openModal(i));
            }
        }

        // Добавляем остальные обработчики
        const closeBtn = document.querySelector('.close-btn');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.closeModal());
        }

        const addBtn = document.querySelector('.add-btn');
        if (addBtn) {
            addBtn.addEventListener('click', () => this.submitForm());
        }
    }

    init() {
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Закрытие по клику вне модального окна
        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.closeModal();
            }
        });

        // Закрытие по Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.modal.classList.contains('active')) {
                this.closeModal();
            }
        });
    }

    openModal(semester) {
        document.getElementById('semester-number').innerText = semester;
        document.getElementById('hiddenSemester').value = semester;
        this.modal.classList.add('active');
        document.body.style.overflow = 'hidden';

        const modalContent = this.modal.querySelector('.modal-content');
        modalContent.style.transform = 'scale(0.9)';
        modalContent.style.opacity = '0';

        requestAnimationFrame(() => {
            modalContent.style.transform = 'scale(1)';
            modalContent.style.opacity = '1';
        });

        this.fetchFiles();
    }

    closeModal() {
        const modalContent = this.modal.querySelector('.modal-content');
        modalContent.style.transform = 'scale(0.9)';
        modalContent.style.opacity = '0';

        setTimeout(() => {
            this.modal.classList.remove('active');
            document.body.style.overflow = '';
            modalContent.style.transform = '';
            modalContent.style.opacity = '';
        }, 300);
    }

    submitForm() {
        const form = document.getElementById('uploadForm');
        const formData = new FormData(form);
        const data = Utils.getDataFromContainer();

        if (!data) {
            Utils.showNotification("Error: University ID not found", "error");
            return;
        }

        formData.append('universityId', data.universityId);

        const fileInput = form.querySelector('input[type="file"]');
        const files = fileInput.files;

        if (files.length === 0) {
            Utils.showNotification("Please select a file to upload", "error");
            return;
        }

        let validFile = false;
        for (let i = 0; i < files.length; i++) {
            if (files[i].size > 0 && files[i].name.trim() !== "") {
                validFile = true;
                break;
            }
        }

        if (!validFile) {
            Utils.showNotification("File cannot be empty", "error");
            return;
        }

        if (!Utils.checkAuthentication()) {
            Utils.showNotification("Authentication required", "error");
            return;
        }

        fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
            }
        })
            .then(async response => {
                if (!response.ok) {
                    const errorText = await response.text();
                    if (response.status === 400 && errorText.includes("already exists")) {
                        throw new Error("File with this name already exists in this semester");
                    }
                    throw new Error(errorText || 'Error uploading file');
                }
                return response.text();
            })
            .then(result => {
                Utils.showNotification("File uploaded successfully!");
                this.fetchFiles();
                form.reset();
            })
            .catch(error => {
                console.error("Error uploading file:", error);
                Utils.showNotification(error.message || "Error uploading file", "error");
            });
    }

    async fetchFiles() {
        if (!Utils.checkAuthentication()) {
            return;
        }

        const data = Utils.getDataFromContainer();
        if (!data) return;

        const url = new URL('/files/grouped', window.location.origin);
        url.searchParams.append('universityId', data.universityId);

        try {
            const response = await Utils.fetchWithAuth(url.toString());
            const files = await response.json();
            this.updateFileList(files);
        } catch (error) {
            console.error("Error loading file list:", error);
            Utils.showNotification("Error loading files", "error");
        }
    }

    updateFileList(groupedFiles) {
        const semesterFileCounts = {};
        const fileListContainer = document.getElementById('file-list-container');
        const currentSemester = parseInt(document.getElementById('hiddenSemester').value, 10);
        fileListContainer.innerHTML = '';

        // Очистка списков файлов для каждого семестра
        for (let i = 1; i <= 8; i++) {
            const semesterContainer = document.querySelector(`#semester-${i} .file-list`);
            if (semesterContainer) {
                semesterContainer.innerHTML = '';
            }
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
                    groupHeader.textContent = `${faculty} - Semester ${semesterNumber}`;
                    fileListContainer.appendChild(groupHeader);

                    const fileList = groupedFiles[group];
                    if (Array.isArray(fileList)) {
                        semesterFileCounts[semesterNumber] += fileList.length;
                        const ul = document.createElement('ul');

                        fileList.forEach(fileName => {
                            if (fileName && fileName.trim() !== "") {
                                const listItem = this.createFileListItem(fileName);
                                ul.appendChild(listItem);
                            }
                        });
                        fileListContainer.appendChild(ul);
                    }
                }
            }
        }
    }

    createFileListItem(fileName) {
        const listItem = document.createElement('li');
        listItem.style.display = 'flex';
        listItem.style.alignItems = 'center';

        const fileNameSpan = document.createElement('span');
        fileNameSpan.textContent = fileName;
        fileNameSpan.style.marginRight = '10px';
        fileNameSpan.style.flex = '1';

        const downloadButton = document.createElement('img');
        downloadButton.src = '/images/download.png';
        downloadButton.alt = 'Download';
        downloadButton.style.cursor = 'pointer';
        downloadButton.style.width = '20px';
        downloadButton.style.verticalAlign = 'middle';

        downloadButton.addEventListener('click', (event) => {
            event.stopPropagation();
            this.downloadFile(fileName);
        });

        listItem.appendChild(fileNameSpan);
        listItem.appendChild(downloadButton);

        listItem.addEventListener('click', () => {
            this.openFileInNewTab(fileName);
        });

        return listItem;
    }

    async downloadFile(fileName) {
        if (!Utils.checkAuthentication()) {
            Utils.showNotification("Authentication required", "error");
            return;
        }

        try {
            const response = await Utils.fetchWithAuth(`/files/download?fileName=${encodeURIComponent(fileName)}`);
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            Utils.showNotification("File download started");
        } catch (error) {
            console.error("Error downloading file:", error);
            Utils.showNotification("Error downloading file", "error");
        }
    }

    async openFileInNewTab(fileName) {
        if (!Utils.checkAuthentication()) {
            Utils.showNotification("Authentication required", "error");
            return;
        }

        const fileUrl = `/files/content?fileName=${encodeURIComponent(fileName)}`;
        try {
            const response = await Utils.fetchWithAuth(fileUrl);
            if (response.ok) {
                window.open(fileUrl, '_blank');
            } else {
                throw new Error('Failed to open file');
            }
        } catch (error) {
            console.error("Error opening file:", error);
            Utils.showNotification("Error opening file", "error");
        }
    }
}


const modalSystem = new ModalSystem();
window.modalSystem = modalSystem; // Делаем доступным глобально
export default modalSystem;