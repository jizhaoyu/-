const API_BASE = new URL("./api/kb", document.baseURI).pathname.replace(/\/$/, "");
const DOCUMENTS_CONTEXT_KEY = "kbAgent.documentsContext";
const MESSAGE_AUTO_HIDE_MS = 3200;
const STATUS_META = {
    READY: { className: "status-ready", label: "已就绪" },
    PROCESSING: { className: "status-processing", label: "索引中" },
    FAILED: { className: "status-failed", label: "失败" }
};
const RUNTIME_STATUS_META = {
    UP: { className: "runtime-up", label: "正常" },
    WARN: { className: "runtime-warn", label: "注意" },
    DOWN: { className: "runtime-down", label: "异常" },
    DISABLED: { className: "runtime-disabled", label: "已关闭" }
};

const state = {
    selectedDocumentId: null,
    documentContent: {
        documentId: null,
        status: "idle",
        content: "",
        error: ""
    },
    pageNum: 1,
    pageSize: 10,
    total: 0,
    trackingTimer: null,
    runtimeOverview: null,
    activeSection: "runtime",
    isLoadingDocuments: false,
    messageTimer: null,
    restoredDocumentsContext: readDocumentsContext(),
    shouldRestoreSelectedDocument: false,
    documentRecords: []
};

const refs = {};

document.addEventListener("DOMContentLoaded", async () => {
    cacheElements();
    bindEvents();
    initializeSectionView();
    syncKbId(refs.uploadKbId.value.trim(), refs.uploadKbId);
    syncQueryModeUI();

    try {
        await loadRuntimeOverview(true);
    } catch (error) {
        handlePageError(error);
    }

    applyStoredDocumentsContext();

    try {
        await loadDocuments(state.pageNum);
    } catch (error) {
        handlePageError(error);
    }
});

function cacheElements() {
    refs.globalMessage = document.getElementById("global-message");
    refs.heroKbId = document.getElementById("hero-kb-id");
    refs.heroSupportedTypes = document.getElementById("hero-supported-types");
    refs.heroVectorMode = document.getElementById("hero-vector-mode");
    refs.heroModelPair = document.getElementById("hero-model-pair");
    refs.directoryNav = document.getElementById("directory-nav");
    refs.directoryLinks = Array.from(document.querySelectorAll("[data-nav-target]"));
    refs.sectionCards = Array.from(document.querySelectorAll("[data-section-id]"));

    refs.runtimeRefresh = document.getElementById("runtime-refresh");
    refs.runtimeStatusGrid = document.getElementById("runtime-status-grid");
    refs.runtimeDefaultKb = document.getElementById("runtime-default-kb");
    refs.runtimeVectorMode = document.getElementById("runtime-vector-mode");
    refs.runtimeCollectionName = document.getElementById("runtime-collection-name");
    refs.runtimeChunkStrategy = document.getElementById("runtime-chunk-strategy");
    refs.runtimeVectorDimension = document.getElementById("runtime-vector-dimension");
    refs.runtimeDocumentSummary = document.getElementById("runtime-document-summary");
    refs.runtimeStoragePath = document.getElementById("runtime-storage-path");
    refs.runtimeSummaryNote = document.getElementById("runtime-summary-note");

    refs.uploadForm = document.getElementById("upload-form");
    refs.uploadKbId = document.getElementById("upload-kb-id");
    refs.uploadTags = document.getElementById("upload-tags");
    refs.uploadFile = document.getElementById("upload-file");
    refs.uploadSubmit = document.getElementById("upload-submit");
    refs.uploadResult = document.getElementById("upload-result");

    refs.queryForm = document.getElementById("query-form");
    refs.queryModeInputs = Array.from(document.querySelectorAll('input[name="queryMode"]'));
    refs.queryModeHint = document.getElementById("query-mode-hint");
    refs.querySectionNote = document.getElementById("query-section-note");
    refs.queryKbId = document.getElementById("query-kb-id");
    refs.queryQuestion = document.getElementById("query-question");
    refs.queryTopKField = document.getElementById("query-top-k-field");
    refs.queryTopK = document.getElementById("query-top-k");
    refs.queryDocIdField = document.getElementById("query-doc-id-field");
    refs.queryDocId = document.getElementById("query-doc-id");
    refs.querySubmit = document.getElementById("query-submit");
    refs.queryResult = document.getElementById("query-result");

    refs.filterForm = document.getElementById("filter-form");
    refs.filterKbId = document.getElementById("filter-kb-id");
    refs.filterStatus = document.getElementById("filter-status");
    refs.filterFileName = document.getElementById("filter-file-name");
    refs.filterPageSize = document.getElementById("filter-page-size");
    refs.filterSubmit = document.getElementById("filter-submit");
    refs.filterRefresh = document.getElementById("filter-refresh");
    refs.filterReset = document.getElementById("filter-reset");
    refs.statusShortcutButtons = Array.from(document.querySelectorAll("[data-status-shortcut]"));
    refs.documentsCurrentKb = document.getElementById("documents-current-kb");
    refs.documentsActiveFilters = document.getElementById("documents-active-filters");
    refs.documentsResultSummary = document.getElementById("documents-result-summary");
    refs.documentsSelectedSummary = document.getElementById("documents-selected-summary");
    refs.documentTableBody = document.getElementById("document-table-body");
    refs.listSummary = document.getElementById("list-summary");
    refs.prevPage = document.getElementById("prev-page");
    refs.nextPage = document.getElementById("next-page");

    refs.documentDetail = document.getElementById("document-detail");
}

function bindEvents() {
    refs.directoryNav.addEventListener("click", handleDirectoryNavigation);
    window.addEventListener("hashchange", handleHashNavigation);
    refs.runtimeRefresh.addEventListener("click", () => loadRuntimeOverview().catch(handlePageError));
    refs.uploadForm.addEventListener("submit", handleUpload);
    refs.queryForm.addEventListener("submit", handleQuery);
    refs.queryModeInputs.forEach((input) => {
        input.addEventListener("change", syncQueryModeUI);
    });
    refs.filterForm.addEventListener("submit", handleFilterSubmit);
    refs.filterRefresh.addEventListener("click", () => loadDocuments().catch(handlePageError));
    refs.filterReset.addEventListener("click", handleFilterReset);
    refs.statusShortcutButtons.forEach((button) => {
        button.addEventListener("click", handleStatusShortcutClick);
    });
    refs.prevPage.addEventListener("click", () => {
        if (!state.isLoadingDocuments && state.pageNum > 1) {
            loadDocuments(state.pageNum - 1).catch(handlePageError);
        }
    });
    refs.nextPage.addEventListener("click", () => {
        const totalPages = Math.max(1, Math.ceil(state.total / state.pageSize));
        if (!state.isLoadingDocuments && state.pageNum < totalPages) {
            loadDocuments(state.pageNum + 1).catch(handlePageError);
        }
    });
    refs.documentTableBody.addEventListener("click", handleTableAction);

    [refs.uploadKbId, refs.queryKbId, refs.filterKbId].forEach((input) => {
        input.addEventListener("input", () => syncKbId(input.value.trim(), input));
    });

    refs.filterPageSize.addEventListener("change", () => {
        state.pageSize = Number(refs.filterPageSize.value) || 10;
        persistDocumentsContext({ pageSize: state.pageSize, pageNum: 1 });
    });
}

function initializeSectionView() {
    const hasRestoredContext = Boolean(state.restoredDocumentsContext);
    const initialSection = hasRestoredContext ? "documents" : resolveSectionFromHash(window.location.hash);
    setActiveSection(initialSection, {
        updateHash: hasRestoredContext,
        scrollIntoView: false,
        persistContext: false
    });
}

function handleDirectoryNavigation(event) {
    const button = event.target.closest("[data-nav-target]");
    if (!button) {
        return;
    }
    setActiveSection(button.dataset.navTarget);
}

function handleHashNavigation() {
    setActiveSection(resolveSectionFromHash(window.location.hash), {
        updateHash: false,
        scrollIntoView: false
    });
}

function resolveSectionFromHash(hash) {
    const rawSectionId = String(hash || "")
        .replace(/^#/, "")
        .replace(/^section-/, "")
        .trim();
    const sectionId = rawSectionId === "detail" ? "documents" : rawSectionId;
    const hasMatch = refs.sectionCards.some((section) => section.dataset.sectionId === sectionId);
    return hasMatch ? sectionId : "runtime";
}

function setActiveSection(sectionId, options = {}) {
    const { updateHash = true, scrollIntoView = true, persistContext = true } = options;
    const nextSection = refs.sectionCards.find((section) => section.dataset.sectionId === sectionId) || refs.sectionCards[0];
    if (!nextSection) {
        return;
    }

    state.activeSection = nextSection.dataset.sectionId;

    refs.sectionCards.forEach((section) => {
        const isActive = section === nextSection;
        section.classList.toggle("is-active", isActive);
        section.setAttribute("aria-hidden", String(!isActive));
    });

    refs.directoryLinks.forEach((link) => {
        const isActive = link.dataset.navTarget === state.activeSection;
        link.classList.toggle("is-active", isActive);
        link.setAttribute("aria-current", isActive ? "page" : "false");
    });

    if (updateHash) {
        const nextHash = `#${state.activeSection}`;
        if (window.location.hash !== nextHash) {
            window.history.replaceState(null, "", nextHash);
        }
    }

    if (persistContext && state.activeSection === "documents") {
        persistDocumentsContext();
    }

    if (scrollIntoView) {
        scrollToElement(nextSection);
    }
}

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function scrollToElement(element, block = "start") {
    if (!element) {
        return;
    }
    element.scrollIntoView({
        behavior: prefersReducedMotion() ? "auto" : "smooth",
        block
    });
}

async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, options);
    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        throw new Error("服务返回了非 JSON 响应");
    }

    if (!response.ok) {
        throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    if (!payload || payload.code !== 200) {
        throw new Error(payload?.message || "请求失败");
    }
    return payload.data;
}

async function loadRuntimeOverview(silent = false) {
    setButtonBusy(refs.runtimeRefresh, true, "刷新中");

    try {
        const overview = await request("/runtime");
        state.runtimeOverview = overview;
        renderRuntimeOverview(overview);
        syncKbId(overview.defaultKbId || refs.uploadKbId.value.trim());
        if (!silent) {
            showMessage("info", "运行态已刷新。");
        }
    } finally {
        setButtonBusy(refs.runtimeRefresh, false, "");
    }
}

function renderRuntimeOverview(overview) {
    const supportedTypes = (overview.supportedFileTypes || []).map((item) => String(item).toUpperCase()).join(" / ") || "PDF / MD / TXT";
    refs.heroSupportedTypes.textContent = supportedTypes;
    refs.heroVectorMode.textContent = overview.vectorStoreMode || "IN_MEMORY";
    refs.heroModelPair.textContent = `${overview.embeddingModel || "未配置"} / ${overview.chatModel || "未配置"}`;

    refs.runtimeDefaultKb.textContent = overview.defaultKbId || "default-kb";
    refs.runtimeVectorMode.textContent = overview.vectorStoreMode || "IN_MEMORY";
    refs.runtimeCollectionName.textContent = overview.milvusCollectionName || "kb_chunk";
    refs.runtimeChunkStrategy.textContent = `${overview.chunkSize || "—"} / ${overview.chunkOverlap || "—"}`;
    refs.runtimeVectorDimension.textContent = String(overview.vectorDimension || "—");
    refs.runtimeStoragePath.textContent = overview.storagePath || "—";
    refs.runtimeSummaryNote.textContent = `Embedding: ${overview.embeddingModel || "未配置"}，Chat: ${overview.chatModel || "未配置"}。`;

    refs.runtimeStatusGrid.innerHTML = renderRuntimeCards(overview.components || []);
}

function renderRuntimeCards(components) {
    if (!components || components.length === 0) {
        return `
            <article class="runtime-status-card runtime-status-card-pending">
                <p>后端没有返回运行态组件数据。</p>
            </article>
        `;
    }

    return components.map((component) => {
        const meta = RUNTIME_STATUS_META[component.status] || RUNTIME_STATUS_META.WARN;
        return `
            <article class="runtime-status-card ${meta.className}">
                <div class="runtime-status-head">
                    <h3>${escapeHtml(component.label || component.key || "组件")}</h3>
                    <span class="runtime-pill ${meta.className}">${meta.label}</span>
                </div>
                <p class="runtime-status-summary">${escapeHtml(component.summary || "—")}</p>
                <p class="runtime-status-detail">${escapeHtml(component.detail || "—")}</p>
            </article>
        `;
    }).join("");
}

function syncKbId(value, sourceInput) {
    const kbId = value || "default-kb";
    refs.heroKbId.textContent = kbId;
    [refs.uploadKbId, refs.queryKbId, refs.filterKbId].forEach((input) => {
        if (input !== sourceInput) {
            input.value = kbId;
        }
    });
}

function getSelectedQueryMode() {
    const selectedInput = refs.queryModeInputs.find((input) => input.checked);
    return selectedInput ? selectedInput.value : "KB";
}

function syncQueryModeUI() {
    const mode = getSelectedQueryMode();
    const isChatMode = mode === "CHAT";

    refs.queryTopK.disabled = isChatMode;
    refs.queryDocId.disabled = isChatMode;
    refs.queryTopKField.classList.toggle("field-disabled", isChatMode);
    refs.queryDocIdField.classList.toggle("field-disabled", isChatMode);

    if (isChatMode) {
        refs.querySectionNote.textContent = "当前为普通聊天，不会使用 embedding 或向量检索。";
        refs.queryModeHint.textContent = "普通聊天会直接调用大模型，页面只展示最终回答结果；Top K 与限定文档 ID 会被忽略。";
        refs.queryDocId.value = "";
        return;
    }

    refs.querySectionNote.textContent = "当前为知识库问答，可选按文档 ID 过滤，便于收敛回答范围。";
    refs.queryModeHint.textContent = "知识库问答会走检索增强流程，页面只展示最终回答结果。";
}

function setButtonBusy(button, isBusy, busyText) {
    if (!button) {
        return;
    }

    if (isBusy) {
        if (!button.dataset.originalText) {
            button.dataset.originalText = button.textContent;
        }
        button.disabled = true;
        button.classList.add("is-loading");
        button.textContent = busyText;
        return;
    }

    button.disabled = false;
    button.classList.remove("is-loading");
    if (button.dataset.originalText) {
        button.textContent = button.dataset.originalText;
    }
}

function setStatusShortcutState(status) {
    refs.statusShortcutButtons.forEach((button) => {
        const isActive = button.dataset.statusShortcut === status;
        button.classList.toggle("is-active", isActive);
        button.setAttribute("aria-pressed", String(isActive));
    });
}

function getActiveFilterSummary() {
    const parts = [];
    const status = refs.filterStatus.value;
    const fileName = refs.filterFileName.value.trim();

    if (status) {
        parts.push(`状态 ${status}`);
    }
    if (fileName) {
        parts.push(`文件名包含 “${fileName}”`);
    }
    if (!parts.length) {
        parts.push("全部文档");
    }

    parts.push(`每页 ${state.pageSize} 条`);
    return parts.join(" · ");
}

function updateDocumentsOverview(records = state.documentRecords) {
    refs.documentsCurrentKb.textContent = refs.filterKbId.value.trim() || "default-kb";
    refs.documentsActiveFilters.textContent = getActiveFilterSummary();
    refs.documentsResultSummary.textContent = state.isLoadingDocuments
        ? "正在加载列表..."
        : `第 ${state.pageNum} 页 · 当前 ${records.length} 条 / 总计 ${state.total} 条`;

    if (!state.selectedDocumentId) {
        refs.documentsSelectedSummary.textContent = "尚未选中文档";
        return;
    }

    const selectedRecord = records.find((item) => normalizeOptionalId(item.documentId) === state.selectedDocumentId);
    if (!selectedRecord) {
        refs.documentsSelectedSummary.textContent = `已选中文档 #${state.selectedDocumentId}`;
        return;
    }

    refs.documentsSelectedSummary.textContent = `#${selectedRecord.documentId} · ${selectedRecord.fileName} · ${selectedRecord.status}`;
}

function resetDocumentDetailPanel(message) {
    state.selectedDocumentId = null;
    state.documentContent = {
        documentId: null,
        status: "idle",
        content: "",
        error: ""
    };
    refs.queryDocId.value = "";
    refs.documentDetail.innerHTML = `
        <div class="detail-empty-state">
            <strong>${escapeHtml(message || "先选一条文档开始查看。")}</strong>
            <p>点击表格行可在下方打开详情面板；点击“详情”按钮会进入独立详情页。</p>
        </div>
    `;
    highlightSelectedRow(null);
    persistDocumentsContext({ selectedDocumentId: null });
    updateDocumentsOverview();
}

function handleFilterReset() {
    refs.filterStatus.value = "";
    refs.filterFileName.value = "";
    refs.filterPageSize.value = "10";
    state.pageNum = 1;
    state.pageSize = 10;
    setStatusShortcutState("");
    persistDocumentsContext({
        status: "",
        fileName: "",
        pageNum: 1,
        pageSize: 10,
        selectedDocumentId: null
    });
    resetDocumentDetailPanel("筛选已清空，重新从列表选择文档。");
    loadDocuments(1).catch(handlePageError);
}

function handleStatusShortcutClick(event) {
    const button = event.currentTarget;
    const status = button.dataset.statusShortcut || "";
    refs.filterStatus.value = status;
    setStatusShortcutState(status);
    state.pageNum = 1;
    persistDocumentsContext({ status, pageNum: 1 });
    loadDocuments(1).catch(handlePageError);
}

function hideMessage() {
    window.clearTimeout(state.messageTimer);
    state.messageTimer = null;
    refs.globalMessage.classList.add("hidden");
}

function showMessage(type, message) {
    window.clearTimeout(state.messageTimer);
    refs.globalMessage.className = `message ${type}`;
    refs.globalMessage.textContent = message;
    refs.globalMessage.classList.remove("hidden");

    if (type !== "error") {
        state.messageTimer = window.setTimeout(() => {
            hideMessage();
        }, MESSAGE_AUTO_HIDE_MS);
    }
}

function handlePageError(error) {
    console.error(error);
    showMessage("error", error.message || "页面加载失败");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function formatDateTime(value) {
    if (!value) {
        return "—";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return new Intl.DateTimeFormat("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    }).format(date);
}

function renderStatus(status) {
    const meta = STATUS_META[status] || { className: "status-processing", label: "处理中" };
    return `
        <span class="status-badge ${meta.className}">
            <span>${escapeHtml(meta.label)}</span>
            <small>${escapeHtml(status || "UNKNOWN")}</small>
        </span>
    `;
}

function renderTags(tags) {
    if (!tags || tags.length === 0) {
        return "—";
    }
    return tags.map((tag) => `<span class="tag-pill">${escapeHtml(tag)}</span>`).join("");
}

function formatSourceType(value) {
    if (!value) {
        return "—";
    }
    return String(value).toUpperCase();
}

function renderDocumentContent() {
    const currentContent = state.documentContent;
    if (!currentContent || currentContent.documentId !== state.selectedDocumentId) {
        return '<p class="document-content-placeholder">选中文档后，这里会加载正文内容。</p>';
    }
    if (currentContent.status === "loading") {
        return '<p class="document-content-loading">正在加载正文内容...</p>';
    }
    if (currentContent.status === "error") {
        return `<p class="document-content-error">正文加载失败：${escapeHtml(currentContent.error || "未知错误")}</p>`;
    }
    if (!currentContent.content) {
        return '<p class="document-content-empty">当前文档没有可展示的正文内容。</p>';
    }
    return `<div class="document-content-body">${escapeHtml(currentContent.content)}</div>`;
}

function updateDocumentContentPanel() {
    const contentPanel = refs.documentDetail.querySelector('[data-role="document-content"]');
    if (contentPanel) {
        contentPanel.innerHTML = renderDocumentContent();
    }
}

function buildDocumentDetailUrl(documentId) {
    return new URL(`./details-${encodeURIComponent(String(documentId).trim())}`, document.baseURI).toString();
}

async function loadDocumentContent(documentId) {
    const normalizedDocumentId = String(documentId).trim();
    try {
        const documentContent = await request(`/documents/${normalizedDocumentId}/content`);
        if (state.selectedDocumentId !== normalizedDocumentId) {
            return;
        }
        state.documentContent = {
            documentId: normalizedDocumentId,
            status: "loaded",
            content: documentContent.content || "",
            error: ""
        };
    } catch (error) {
        if (state.selectedDocumentId !== normalizedDocumentId) {
            return;
        }
        state.documentContent = {
            documentId: normalizedDocumentId,
            status: "error",
            content: "",
            error: error.message || "请求失败"
        };
    }
    updateDocumentContentPanel();
}

async function handleUpload(event) {
    event.preventDefault();
    if (!refs.uploadFile.files || refs.uploadFile.files.length === 0) {
        showMessage("error", "请选择要上传的文件");
        return;
    }

    const formData = new FormData();
    formData.append("kbId", refs.uploadKbId.value.trim());
    formData.append("tags", refs.uploadTags.value.trim());
    formData.append("file", refs.uploadFile.files[0]);

    setButtonBusy(refs.uploadSubmit, true, "上传中");
    refs.uploadResult.innerHTML = "<p>正在上传文件并触发异步索引...</p>";

    try {
        const result = await request("/documents", {
            method: "POST",
            body: formData
        });
        const detailUrl = buildDocumentDetailUrl(result.documentId);
        const normalizedDocumentId = normalizeOptionalId(result.documentId);

        refs.uploadResult.innerHTML = `
            <div class="detail-title">
                <h3>上传已提交</h3>
                ${renderStatus(result.status)}
            </div>
            <div class="meta-grid">
                <div class="meta-item">
                    <span>文档 ID</span>
                    <strong>${escapeHtml(result.documentId)}</strong>
                </div>
                <div class="meta-item">
                    <span>Trace ID</span>
                    <strong>${escapeHtml(result.traceId)}</strong>
                </div>
                <div class="meta-item">
                    <span>下一步</span>
                    <strong><a class="inline-link" href="${escapeHtml(detailUrl)}">打开独立详情页</a></strong>
                </div>
            </div>
        `;
        refs.uploadFile.value = "";
        showMessage("success", `文档 #${result.documentId} 已上传，正在索引。`);
        persistDocumentsContext({
            activeSection: "documents",
            pageNum: 1,
            selectedDocumentId: normalizedDocumentId,
            kbId: refs.filterKbId.value.trim()
        });
        await loadDocuments(1);
        if (normalizedDocumentId) {
            await loadDocumentDetail(normalizedDocumentId, true);
            trackDocumentStatus(normalizedDocumentId);
        }
    } catch (error) {
        refs.uploadResult.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
        handlePageError(error);
    } finally {
        setButtonBusy(refs.uploadSubmit, false, "");
    }
}

async function handleQuery(event) {
    event.preventDefault();

    const mode = getSelectedQueryMode();
    const payload = {
        kbId: refs.queryKbId.value.trim(),
        question: refs.queryQuestion.value.trim(),
        mode,
        topK: Number(refs.queryTopK.value) || 5,
        metadataFilters: {}
    };

    if (mode === "KB" && refs.queryDocId.value.trim()) {
        payload.metadataFilters.docId = refs.queryDocId.value.trim();
    }

    setButtonBusy(refs.querySubmit, true, "查询中");
    refs.queryResult.innerHTML = mode === "CHAT"
        ? "<p>正在调用普通聊天能力生成回答...</p>"
        : "<p>正在检索知识片段并生成回答...</p>";

    try {
        const result = await request("/query", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        refs.queryResult.innerHTML = `<div class="answer-copy">${escapeHtml(result.answer)}</div>`;
        showMessage("success", "问答请求已完成。");
    } catch (error) {
        refs.queryResult.innerHTML = `<p>${escapeHtml(error.message)}</p>`;
        handlePageError(error);
    } finally {
        setButtonBusy(refs.querySubmit, false, "");
    }
}

async function handleFilterSubmit(event) {
    event.preventDefault();
    try {
        state.pageNum = 1;
        setStatusShortcutState(refs.filterStatus.value);
        await loadDocuments(1);
    } catch (error) {
        handlePageError(error);
    }
}

async function loadDocuments(pageNum = state.pageNum) {
    state.pageNum = normalizePositiveInt(pageNum, 1);
    state.pageSize = normalizePositiveInt(Number(refs.filterPageSize.value), state.pageSize || 10);
    refs.filterPageSize.value = String(state.pageSize);
    setStatusShortcutState(refs.filterStatus.value);
    setDocumentListBusy(true);
    refs.documentTableBody.innerHTML = `
        <tr>
            <td colspan="6" class="table-placeholder">正在加载文档列表...</td>
        </tr>
    `;
    updateDocumentsOverview();

    const params = new URLSearchParams({
        kbId: refs.filterKbId.value.trim(),
        pageNum: String(state.pageNum),
        pageSize: String(state.pageSize)
    });

    if (refs.filterStatus.value) {
        params.append("status", refs.filterStatus.value);
    }
    if (refs.filterFileName.value.trim()) {
        params.append("fileName", refs.filterFileName.value.trim());
    }

    try {
        const page = await request(`/documents?${params.toString()}`);
        const records = page.records || [];

        state.documentRecords = records;
        state.total = Number(page.total) || 0;
        state.pageNum = normalizePositiveInt(page.pageNum, 1);
        state.pageSize = normalizePositiveInt(page.pageSize, state.pageSize);
        refs.filterPageSize.value = String(state.pageSize);

        renderDocumentTable(records);
        updateDocumentSummary(records, state.total);
        updatePagination();
        updateDocumentsOverview(records);
        persistDocumentsContext();
        await restoreSelectedDocumentPanelIfNeeded(records);
        updateDocumentsOverview(records);
    } finally {
        setDocumentListBusy(false);
        updatePagination();
        updateDocumentsOverview();
    }
}

function setDocumentListBusy(isBusy) {
    state.isLoadingDocuments = isBusy;
    refs.prevPage.disabled = isBusy;
    refs.nextPage.disabled = isBusy;
}

function updateDocumentSummary(records, total) {
    const counts = { READY: 0, PROCESSING: 0, FAILED: 0 };
    (records || []).forEach((item) => {
        if (counts[item.status] !== undefined) {
            counts[item.status] += 1;
        }
    });
    refs.runtimeDocumentSummary.textContent = `总数 ${total || 0} / 当前页 READY ${counts.READY} · PROCESSING ${counts.PROCESSING} · FAILED ${counts.FAILED}`;
}

function renderDocumentTable(records) {
    if (!records || records.length === 0) {
        refs.documentTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="table-placeholder">当前筛选条件下没有文档，请调整筛选条件或清空筛选后重试。</td>
            </tr>
        `;
        return;
    }

    refs.documentTableBody.innerHTML = records.map((doc) => {
        const documentId = normalizeOptionalId(doc.documentId);
        const isActive = documentId && documentId === state.selectedDocumentId;
        return `
            <tr class="${isActive ? "active-row" : ""}" data-document-row data-id="${escapeHtml(doc.documentId)}" data-testid="documents-row-${escapeHtml(doc.documentId)}" aria-selected="${isActive ? "true" : "false"}" tabindex="0">
                <td>${escapeHtml(doc.documentId)}</td>
                <td>
                    <strong>${escapeHtml(doc.fileName)}</strong><br>
                    <span>${escapeHtml(formatSourceType(doc.sourceType))}</span>
                </td>
                <td>${renderStatus(doc.status)}</td>
                <td><div class="tag-list">${renderTags(doc.tags)}</div></td>
                <td>${escapeHtml(formatDateTime(doc.updatedAt))}</td>
                <td>
                    <div class="inline-actions">
                        <button class="table-action" type="button" data-action="detail" data-id="${escapeHtml(doc.documentId)}" data-testid="documents-open-detail-${escapeHtml(doc.documentId)}">详情</button>
                        <button class="table-action" type="button" data-action="reindex" data-id="${escapeHtml(doc.documentId)}" data-testid="documents-reindex-${escapeHtml(doc.documentId)}">重建</button>
                        <button class="table-action" type="button" data-tone="danger" data-action="delete" data-id="${escapeHtml(doc.documentId)}" data-testid="documents-delete-${escapeHtml(doc.documentId)}">删除</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

function updatePagination() {
    const totalPages = Math.max(1, Math.ceil(state.total / state.pageSize));
    refs.listSummary.textContent = `共 ${state.total} 条，当前第 ${state.pageNum} / ${totalPages} 页，每页 ${state.pageSize} 条。`;
    refs.prevPage.disabled = state.isLoadingDocuments || state.pageNum <= 1;
    refs.nextPage.disabled = state.isLoadingDocuments || state.pageNum >= totalPages;
}

async function handleTableAction(event) {
    const button = event.target.closest("button[data-action]");
    if (button) {
        const action = button.dataset.action;
        const documentId = normalizeOptionalId(button.dataset.id);
        if (!documentId) {
            return;
        }

        try {
            if (action === "detail") {
                persistDocumentsContext({
                    activeSection: "documents",
                    selectedDocumentId: documentId,
                    pageNum: state.pageNum,
                    pageSize: state.pageSize
                });
                window.location.href = buildDocumentDetailUrl(documentId);
                return;
            }

            if (action === "reindex") {
                await reindexDocument(documentId, button);
                return;
            }

            if (action === "delete") {
                await deleteDocument(documentId, button);
            }
        } catch (error) {
            handlePageError(error);
        }
        return;
    }

    const row = event.target.closest("tr[data-document-row]");
    if (!row) {
        return;
    }

    const documentId = normalizeOptionalId(row.dataset.id);
    if (!documentId || state.isLoadingDocuments) {
        return;
    }

    loadDocumentDetail(documentId).catch(handlePageError);
}

async function loadDocumentDetail(documentId, silent = false) {
    const normalizedDocumentId = normalizeOptionalId(documentId);
    if (!normalizedDocumentId) {
        return;
    }

    const documentDetail = await request(`/documents/${normalizedDocumentId}`);
    state.selectedDocumentId = normalizedDocumentId;
    state.documentContent = {
        documentId: normalizedDocumentId,
        status: "loading",
        content: "",
        error: ""
    };
    highlightSelectedRow(normalizedDocumentId);
    renderDocumentDetail(documentDetail);
    persistDocumentsContext({ selectedDocumentId: normalizedDocumentId });
    loadDocumentContent(normalizedDocumentId).catch((error) => console.error(error));
    if (!silent) {
        showMessage("info", `已加载文档 #${normalizedDocumentId} 的详情。`);
    }
}

function renderDocumentDetail(documentDetail) {
    const errorBlockClass = documentDetail.errorMessage ? "detail-copy has-error" : "detail-copy";
    const detailUrl = buildDocumentDetailUrl(documentDetail.documentId);

    refs.documentDetail.innerHTML = `
        <div class="detail-title">
            <h3 data-testid="document-detail-title">${escapeHtml(documentDetail.fileName || "文档详情")}</h3>
            ${renderStatus(documentDetail.status)}
        </div>
        <p class="detail-page-doc-id" data-testid="document-detail-id">文档 ID #${escapeHtml(documentDetail.documentId)} · <a class="inline-link" href="${escapeHtml(detailUrl)}" data-testid="document-detail-link">独立详情页</a></p>
        <div class="meta-grid">
            <div class="meta-item">
                <span>文档 ID</span>
                <strong>${escapeHtml(documentDetail.documentId)}</strong>
            </div>
            <div class="meta-item">
                <span>知识库 ID</span>
                <strong>${escapeHtml(documentDetail.kbId)}</strong>
            </div>
            <div class="meta-item">
                <span>文件类型</span>
                <strong>${escapeHtml(formatSourceType(documentDetail.sourceType))}</strong>
            </div>
            <div class="meta-item">
                <span>标签</span>
                <div class="tag-list">${documentDetail.tags && documentDetail.tags.length ? renderTags(documentDetail.tags) : "—"}</div>
            </div>
            <div class="meta-item">
                <span>创建时间</span>
                <strong>${escapeHtml(formatDateTime(documentDetail.createdAt))}</strong>
            </div>
            <div class="meta-item">
                <span>索引时间</span>
                <strong>${escapeHtml(formatDateTime(documentDetail.indexedAt))}</strong>
            </div>
        </div>
        <div class="${errorBlockClass}" data-testid="document-detail-error-block">
            ${documentDetail.errorMessage
                ? `<strong>错误信息：</strong> ${escapeHtml(documentDetail.errorMessage)}`
                : "当前文档没有错误信息。"}
        </div>
        <section class="detail-section">
            <div class="detail-section-head">
                <h4>文档正文</h4>
                <p class="detail-section-note">按纯文本显示，避免执行文档中的 HTML 或脚本。</p>
            </div>
            <div class="document-content-panel" data-role="document-content" data-testid="document-detail-content">${renderDocumentContent()}</div>
        </section>
    `;
    updateDocumentsOverview();
}

async function reindexDocument(documentId, button) {
    const normalizedDocumentId = normalizeOptionalId(documentId);
    if (!normalizedDocumentId) {
        return;
    }

    setButtonBusy(button, true, "处理中");
    try {
        const result = await request(`/reindex/${normalizedDocumentId}`, {
            method: "POST"
        });
        showMessage("success", `文档 #${normalizedDocumentId} 已重新进入索引流程。`);
        persistDocumentsContext({ selectedDocumentId: normalizedDocumentId });
        await loadDocuments(state.pageNum);
        await loadDocumentDetail(normalizedDocumentId, true);
        trackDocumentStatus(result.documentId);
    } catch (error) {
        handlePageError(error);
    } finally {
        setButtonBusy(button, false, "");
    }
}

async function deleteDocument(documentId, button) {
    const normalizedDocumentId = normalizeOptionalId(documentId);
    if (!normalizedDocumentId) {
        return;
    }

    const confirmed = window.confirm(`确认删除文档 #${normalizedDocumentId} 吗？这会同时删除索引和本地存储文件。`);
    if (!confirmed) {
        return;
    }

    setButtonBusy(button, true, "删除中");
    try {
        await request(`/documents/${normalizedDocumentId}`, {
            method: "DELETE"
        });
        showMessage("success", `文档 #${normalizedDocumentId} 已删除。`);
        if (state.selectedDocumentId === normalizedDocumentId) {
            resetDocumentDetailPanel("已删除当前跟踪文档。");
        }
        const totalPages = Math.max(1, Math.ceil(Math.max(state.total - 1, 0) / state.pageSize));
        const targetPage = Math.min(state.pageNum, totalPages);
        await loadDocuments(targetPage);
    } catch (error) {
        handlePageError(error);
    } finally {
        setButtonBusy(button, false, "");
    }
}

function trackDocumentStatus(documentId, attempts = 20) {
    const normalizedDocumentId = normalizeOptionalId(documentId);
    if (!normalizedDocumentId) {
        return;
    }

    window.clearTimeout(state.trackingTimer);

    const check = async (remaining) => {
        try {
            const documentDetail = await request(`/documents/${normalizedDocumentId}`);
            if (state.selectedDocumentId === normalizedDocumentId) {
                renderDocumentDetail(documentDetail);
            }
            await loadDocuments(state.pageNum);

            if (documentDetail.status === "PROCESSING" && remaining > 1) {
                state.trackingTimer = window.setTimeout(() => {
                    check(remaining - 1).catch((error) => console.error(error));
                }, 3000);
                return;
            }

            if (documentDetail.status !== "PROCESSING") {
                showMessage(
                    documentDetail.status === "READY" ? "success" : "error",
                    `文档 #${normalizedDocumentId} 当前状态：${documentDetail.status}`
                );
                return;
            }

            if (remaining <= 1) {
                showMessage("info", `文档 #${normalizedDocumentId} 仍在索引中，可稍后刷新运行态或文档列表继续观察。`);
            }
        } catch (error) {
            console.error(error);
        }
    };

    check(attempts).catch((error) => console.error(error));
}

function highlightSelectedRow(documentId) {
    const normalizedDocumentId = normalizeOptionalId(documentId);
    refs.documentTableBody.querySelectorAll("tr[data-document-row]").forEach((row) => {
        row.classList.remove("active-row");
        row.setAttribute("aria-selected", "false");
    });
    if (!normalizedDocumentId) {
        return;
    }
    const selectedRow = refs.documentTableBody.querySelector(`tr[data-document-row][data-id="${CSS.escape(normalizedDocumentId)}"]`);
    if (selectedRow) {
        selectedRow.classList.add("active-row");
        selectedRow.setAttribute("aria-selected", "true");
    }
}

function applyStoredDocumentsContext() {
    const savedContext = state.restoredDocumentsContext;
    if (!savedContext) {
        setStatusShortcutState(refs.filterStatus.value);
        updateDocumentsOverview();
        return;
    }

    const filter = savedContext.filter || {};
    if (typeof filter.kbId === "string" && filter.kbId.trim()) {
        refs.filterKbId.value = filter.kbId.trim();
        syncKbId(filter.kbId.trim(), refs.filterKbId);
    }
    refs.filterStatus.value = typeof filter.status === "string" ? filter.status : "";
    refs.filterFileName.value = typeof filter.fileName === "string" ? filter.fileName : "";

    state.pageNum = normalizePositiveInt(savedContext.pageNum, 1);
    state.pageSize = normalizePositiveInt(savedContext.pageSize, Number(refs.filterPageSize.value) || 10);
    refs.filterPageSize.value = String(state.pageSize);

    state.selectedDocumentId = normalizeOptionalId(savedContext.selectedDocumentId);
    state.shouldRestoreSelectedDocument = Boolean(state.selectedDocumentId);
    setStatusShortcutState(refs.filterStatus.value);
    updateDocumentsOverview();
}

function readDocumentsContext() {
    try {
        const rawValue = window.sessionStorage.getItem(DOCUMENTS_CONTEXT_KEY);
        if (!rawValue) {
            return null;
        }
        const parsed = JSON.parse(rawValue);
        if (!parsed || typeof parsed !== "object") {
            return null;
        }
        return parsed;
    } catch (error) {
        console.warn("无法读取文档中心上下文", error);
        return null;
    }
}

function persistDocumentsContext(overrides = {}) {
    try {
        const kbId = typeof overrides.kbId === "string" ? overrides.kbId.trim() : refs.filterKbId.value.trim();
        const nextContext = {
            activeSection: overrides.activeSection || "documents",
            filter: {
                kbId: kbId || "default-kb",
                status: typeof overrides.status === "string" ? overrides.status : refs.filterStatus.value,
                fileName: typeof overrides.fileName === "string" ? overrides.fileName : refs.filterFileName.value.trim()
            },
            pageNum: normalizePositiveInt(overrides.pageNum, state.pageNum),
            pageSize: normalizePositiveInt(overrides.pageSize, Number(refs.filterPageSize.value) || state.pageSize || 10),
            selectedDocumentId: normalizeOptionalId(
                overrides.selectedDocumentId !== undefined ? overrides.selectedDocumentId : state.selectedDocumentId
            )
        };
        window.sessionStorage.setItem(DOCUMENTS_CONTEXT_KEY, JSON.stringify(nextContext));
    } catch (error) {
        console.warn("无法保存文档中心上下文", error);
    }
}

async function restoreSelectedDocumentPanelIfNeeded(records) {
    if (!state.shouldRestoreSelectedDocument || !state.selectedDocumentId) {
        highlightSelectedRow(state.selectedDocumentId);
        return;
    }

    const shouldRestore = (records || []).some((item) => normalizeOptionalId(item.documentId) === state.selectedDocumentId);
    state.shouldRestoreSelectedDocument = false;

    if (!shouldRestore) {
        highlightSelectedRow(state.selectedDocumentId);
        return;
    }

    await loadDocumentDetail(state.selectedDocumentId, true);
}

function normalizePositiveInt(value, fallback) {
    const normalizedValue = Number(value);
    if (Number.isInteger(normalizedValue) && normalizedValue > 0) {
        return normalizedValue;
    }
    return fallback;
}

function normalizeOptionalId(value) {
    const normalizedValue = String(value ?? "").trim();
    return normalizedValue ? normalizedValue : null;
}
