/**
 * 企业办公物资采购平台 · 管理端
 * 技术栈：Vue 3 + Element Plus + ECharts（均为 CDN 全局构建，无需打包）
 *
 * 与后端约定：
 *  - 所有请求经 nginx 反向代理，前端统一加 /api 前缀 → 后端 /admin/**
 *  - 管理端令牌请求头名为 token（员工端是 authentication），两端隔离
 *  - 后端统一返回 { code, msg, data }，code === 1 为成功
 */
const { createApp } = Vue;

const api = {
  tokenKey: 'cyt_admin_token',
  userKey: 'cyt_admin_user',

  getToken() { return localStorage.getItem(this.tokenKey); },

  setAuth(token, user) {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
  },

  clearAuth() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  },

  async request(method, url, data, params) {
    const headers = { 'Content-Type': 'application/json' };
    const token = this.getToken();
    if (token) headers['token'] = token;

    let fullUrl = '/api' + url;
    if (params) {
      const qs = Object.keys(params)
        .filter((k) => params[k] !== null && params[k] !== undefined && params[k] !== '')
        .map((k) => encodeURIComponent(k) + '=' + encodeURIComponent(params[k]))
        .join('&');
      if (qs) fullUrl += '?' + qs;
    }

    let res;
    try {
      res = await fetch(fullUrl, {
        method, headers, body: data ? JSON.stringify(data) : undefined,
      });
    } catch (netErr) {
      // fetch 本身失败时后端不会有任何记录，调用方的 catch(e){} 又会吞掉异常，
      // 这里必须显式提示，否则表现为"点按钮没反应"
      ElementPlus.ElMessage.error('网络异常，未能连接后端服务，请确认服务已启动后重试');
      throw netErr;
    }

    if (res.status === 401) {
      this.clearAuth();
      ElementPlus.ElMessage.error('登录已过期，请重新登录');
      setTimeout(() => location.reload(), 800);
      throw new Error('unauthorized');
    }

    const json = await res.json();
    if (json.code !== 1) {
      ElementPlus.ElMessage.error(json.msg || '操作失败');
      throw new Error(json.msg || 'error');
    }
    return json.data;
  },

  get(url, params) { return this.request('GET', url, null, params); },
  post(url, data, params) { return this.request('POST', url, data, params); },
  put(url, data, params) { return this.request('PUT', url, data, params); },
  del(url, params) { return this.request('DELETE', url, null, params); },
};

const ROLE_TEXT = { EMPLOYEE: '普通员工', PURCHASER: '采购专员', ADMIN: '行政管理员' };
const ROLE_TAG = { EMPLOYEE: 'info', PURCHASER: 'primary', ADMIN: 'danger' };

const ORDER_STATUS_TEXT = {
  1: '待提交', 2: '待审批', 3: '采购中', 4: '配送中', 5: '已完成', 6: '已取消',
};
const ORDER_STATUS_TAG = {
  1: 'info', 2: 'warning', 3: 'primary', 4: 'primary', 5: 'success', 6: 'info',
};

createApp({
  data() {
    return {
      // 登录
      user: null,
      loginForm: { username: '', password: '' },
      loginLoading: false,

      // 导航
      page: 'home',
      menuGroups: [
        { group: '概览', items: [{ key: 'home', label: '工作台', icon: '🏠' }] },
        {
          group: '采购业务',
          items: [
            { key: 'order', label: '申领单管理', icon: '📋' },
            { key: 'goods', label: '物资管理', icon: '📦' },
            { key: 'combo', label: '组合申领包', icon: '🎁' },
            { key: 'category', label: '物资分类', icon: '🗂️' },
          ],
        },
        {
          group: '组织与预算',
          items: [
            { key: 'employee', label: '员工管理', icon: '👥' },
            { key: 'department', label: '部门管理', icon: '🏢' },
            { key: 'budget', label: '部门预算', icon: '💰' },
          ],
        },
      ],

      // 工作台
      biz: {}, ov: {}, goodsOv: {}, comboOv: {},
      shopStatus: 1,

      // 分类
      categories: [], catPage: 1, catPageSize: 10, catTotal: 0,
      catQuery: { type: null, name: '' },
      catDialog: false, catForm: {},

      // 物资
      goodsList: [], goodsPage: 1, goodsTotal: 0, goodsSelection: [],
      goodsQuery: { name: '', status: null, categoryId: null },
      goodsCategories: [], allGoodsList: [],
      goodsDialog: false, goodsForm: { flavors: [] },

      // 组合包
      combos: [], comboPage: 1, comboTotal: 0, comboSelection: [],
      comboQuery: { name: '', status: null, categoryId: null },
      comboCategories: [],
      comboDialog: false, comboForm: { comboItems: [] },

      // 申领单
      orders: [], orderPage: 1, orderTotal: 0, orderStat: {},
      orderQuery: { number: '', phone: '', status: null },
      orderDateRange: null,
      orderDrawer: false, currentOrder: null,
      rejectDialog: false, rejectForm: {},
      cancelDialog: false, cancelForm: {},

      // 员工
      employees: [], empPage: 1, empTotal: 0,
      empQuery: { name: '', role: null, deptId: null },
      empDialog: false, empForm: {},

      // 部门
      departments: [], departmentList: [],
      deptQuery: { name: '' },
      deptDialog: false, deptForm: {},

      // 预算
      budgetList: [], budgetQuery: { deptId: null, period: null },
      budgetDialog: false, budgetForm: {},

      saving: false,
    };
  },

  computed: {
    currentMenuLabel() {
      for (const g of this.menuGroups) {
        const m = g.items.find((x) => x.key === this.page);
        if (m) return m.label;
      }
      return '';
    },
    roleLabel() { return ROLE_TEXT[this.user && this.user.role] || ''; },
  },

  mounted() {
    const cached = localStorage.getItem(api.userKey);
    if (cached && api.getToken()) {
      try {
        this.user = JSON.parse(cached);
        this.bootstrap();
      } catch (e) {
        api.clearAuth();
        this.user = null;
      }
    }
  },

  methods: {
    // ---------------- 登录 ----------------
    async doLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        ElementPlus.ElMessage.warning('请输入账号和密码');
        return;
      }
      this.loginLoading = true;
      try {
        const user = await api.post('/employee/login', this.loginForm);
        api.setAuth(user.token, user);
        this.user = user;
        ElementPlus.ElMessage.success('登录成功');
        this.bootstrap();
      } catch (e) { /* 已在 request 统一提示 */ } finally {
        this.loginLoading = false;
      }
    },

    async doLogout() {
      try { await api.post('/employee/logout'); } catch (e) { /* 后端登出失败不阻塞本地清理 */ }
      api.clearAuth();
      this.user = null;
    },

    /** 登录后统一预加载：部门用于下拉，物资用于组合包选择 */
    async bootstrap() {
      this.loadHome();
      this.loadShopStatus();
      try {
        this.departments = (await api.get('/department/listEnabled')) || [];
      } catch (e) { /* ignore */ }
    },

    // ---------------- 导航 ----------------
    go(key) {
      this.page = key;
      const map = {
        home: () => this.loadHome(),
        order: () => { this.loadOrders(1); this.loadOrderStat(); },
        goods: () => { this.loadGoodsCategories(); this.loadGoodsList(1); this.loadAllGoodsList(); },
        combo: () => { this.loadComboCategories(); this.loadCombos(1); this.loadAllGoodsList(); },
        category: () => this.loadCategories(1),
        employee: () => { this.ensureDepartments(); this.loadEmployees(1); },
        department: () => { this.ensureDepartments(); this.loadDepartments(); this.loadEmployees(1); },
        budget: () => { this.ensureDepartments(); this.loadBudgets(); },
      };
      (map[key] || (() => {}))();
    },

    async ensureDepartments() {
      if (this.departments.length) return;
      try { this.departments = (await api.get('/department/listEnabled')) || []; } catch (e) { /* ignore */ }
    },

    deptName(id) {
      const d = this.departments.find((x) => x.id === id);
      return d ? d.name : '—';
    },

    // ---------------- 采购开关 ----------------
    async loadShopStatus() {
      try {
        const s = await api.get('/shop/status');
        this.shopStatus = s === null || s === undefined ? 1 : s;
      } catch (e) { /* ignore */ }
    },

    async toggleShop() {
      const next = this.shopStatus === 1 ? 0 : 1;
      try {
        await api.put('/shop/' + next);
        this.shopStatus = next;
        ElementPlus.ElMessage.success(next === 1 ? '已切换为采购开放' : '已切换为采购关闭');
      } catch (e) { /* ignore */ }
    },

    // ---------------- 工作台 ----------------
    async loadHome() {
      try {
        this.biz = (await api.get('/workspace/businessData')) || {};
        this.ov = (await api.get('/workspace/overviewOrders')) || {};
        this.goodsOv = (await api.get('/workspace/overviewGoods')) || {};
        this.comboOv = (await api.get('/workspace/overviewCombos')) || {};
      } catch (e) { /* ignore */ }
    },

    // ---------------- 分类 ----------------
    async loadCategories(page) {
      this.catPage = page || 1;
      try {
        const r = await api.get('/category/page', {
          page: this.catPage, pageSize: this.catPageSize,
          type: this.catQuery.type, name: this.catQuery.name,
        });
        this.categories = (r && r.records) || [];
        this.catTotal = (r && r.total) || 0;
      } catch (e) { /* ignore */ }
    },

    openCategory(row) {
      this.catForm = row
        ? { id: row.id, name: row.name, type: row.type, sort: row.sort }
        : { name: '', type: 1, sort: 0 };
      this.catDialog = true;
    },

    async saveCategory() {
      if (!this.catForm.name) { ElementPlus.ElMessage.warning('请输入分类名称'); return; }
      this.saving = true;
      try {
        if (this.catForm.id) await api.put('/category', this.catForm);
        else await api.post('/category', this.catForm);
        ElementPlus.ElMessage.success('保存成功');
        this.catDialog = false;
        this.loadCategories(this.catPage);
      } catch (e) { /* ignore */ } finally { this.saving = false; }
    },

    async toggleCategory(row) {
      const next = row.status === 1 ? 0 : 1;
      try {
        // 后端该接口为 POST /admin/category/status/{status}，id 走查询参数
        await api.post('/category/status/' + next, null, { id: row.id });
        ElementPlus.ElMessage.success('已更新状态');
        this.loadCategories(this.catPage);
      } catch (e) { /* ignore */ }
    },

    async deleteCategory(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确定删除该分类吗？关联了物资或组合包时不允许删除。', '提示',
          { type: 'warning' });
        await api.del('/category', { id: row.id });
        ElementPlus.ElMessage.success('已删除');
        this.loadCategories(this.catPage);
      } catch (e) { /* ignore */ }
    },

    // ---------------- 物资 ----------------
    async loadGoodsCategories() {
      try {
        this.goodsCategories = (await api.get('/category/list', { type: 1 })) || [];
      } catch (e) { /* ignore */ }
    },

    async loadComboCategories() {
      try {
        this.comboCategories = (await api.get('/category/list', { type: 2 })) || [];
      } catch (e) { /* ignore */ }
    },

    async loadAllGoodsList() {
      try {
        this.allGoodsList = (await api.get('/goods/list')) || [];
      } catch (e) { /* ignore */ }
    },

    pickGoodsCategory(id) {
      this.goodsQuery.categoryId = id;
      this.loadGoodsList(1);
    },

    async loadGoodsList(page) {
      this.goodsPage = page || 1;
      try {
        const r = await api.get('/goods/page', {
          page: this.goodsPage, pageSize: 10,
          name: this.goodsQuery.name, status: this.goodsQuery.status,
          categoryId: this.goodsQuery.categoryId,
        });
        this.goodsList = (r && r.records) || [];
        this.goodsTotal = (r && r.total) || 0;
      } catch (e) { /* ignore */ }
    },

    async openGoods(row) {
      if (row) {
        try {
          const d = await api.get('/goods/' + row.id);
          this.goodsForm = {
            id: d.id, name: d.name, categoryId: d.categoryId, price: d.price,
            image: d.image, description: d.description, spec: d.spec, unit: d.unit,
            flavors: (d.flavors || []).map((f) => ({ name: f.name, valueText: f.value })),
          };
        } catch (e) { return; }
      } else {
        this.goodsForm = {
          name: '', categoryId: null, price: 0, image: '', description: '',
          spec: '', unit: '', flavors: [],
        };
      }
      this.goodsDialog = true;
    },

    addFlavorRow() {
      this.goodsForm.flavors.push({ name: '', valueText: '' });
    },

    async saveGoods() {
      if (!this.goodsForm.name || !this.goodsForm.categoryId) {
        ElementPlus.ElMessage.warning('物资名称与分类为必填项');
        return;
      }
      const payload = Object.assign({}, this.goodsForm, {
        flavors: (this.goodsForm.flavors || [])
          .filter((f) => f.name && f.valueText)
          .map((f) => ({ name: f.name, value: f.valueText })),
      });
      this.saving = true;
      try {
        if (payload.id) await api.put('/goods', payload);
        else await api.post('/goods', payload);
        ElementPlus.ElMessage.success('保存成功');
        this.goodsDialog = false;
        this.loadGoodsList(this.goodsPage);
        this.loadAllGoodsList();
      } catch (e) { /* ignore */ } finally { this.saving = false; }
    },

    async toggleGoods(row) {
      const next = row.status === 1 ? 0 : 1;
      try {
        await api.post('/goods/status/' + next, null, { id: row.id });
        ElementPlus.ElMessage.success('已更新状态');
        this.loadGoodsList(this.goodsPage);
      } catch (e) { /* ignore */ }
    },

    async deleteGoods(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确定删除该物资吗？', '提示', { type: 'warning' });
        await api.del('/goods', { ids: row.id });
        ElementPlus.ElMessage.success('已删除');
        this.loadGoodsList(this.goodsPage);
      } catch (e) { /* ignore */ }
    },

    async deleteGoodsList() {
      if (!this.goodsSelection.length) return;
      try {
        await ElementPlus.ElMessageBox.confirm(
          `确定删除选中的 ${this.goodsSelection.length} 项物资吗？已启用或被组合包引用的物资不允许删除。`,
          '提示', { type: 'warning' });
        await api.del('/goods', { ids: this.goodsSelection.map((d) => d.id).join(',') });
        ElementPlus.ElMessage.success('已删除');
        this.loadGoodsList(this.goodsPage);
      } catch (e) { /* ignore */ }
    },

    async uploadImage(file) {
      const fd = new FormData();
      fd.append('file', file);
      try {
        const res = await fetch('/api/common/upload', {
          method: 'POST', headers: { token: api.getToken() }, body: fd,
        });
        const json = await res.json();
        if (json.code === 1) {
          this.goodsForm.image = json.data;
          ElementPlus.ElMessage.success('上传成功');
        } else {
          ElementPlus.ElMessage.error(json.msg || '上传失败');
        }
      } catch (e) {
        ElementPlus.ElMessage.error('上传失败');
      }
      return false;
    },

    // ---------------- 组合包 ----------------
    async loadCombos(page) {
      this.comboPage = page || 1;
      try {
        const r = await api.get('/combo/page', {
          page: this.comboPage, pageSize: 10,
          name: this.comboQuery.name, status: this.comboQuery.status,
          categoryId: this.comboQuery.categoryId,
        });
        this.combos = (r && r.records) || [];
        this.comboTotal = (r && r.total) || 0;
      } catch (e) { /* ignore */ }
    },

    async openCombo(row) {
      if (row) {
        try {
          const d = await api.get('/combo/' + row.id);
          this.comboForm = {
            id: d.id, name: d.name, categoryId: d.categoryId, price: d.price,
            description: d.description, image: d.image,
            comboItems: (d.comboItems || []).map((x) => ({
              goodsId: x.goodsId, name: x.name, price: x.price, copies: x.copies,
            })),
          };
        } catch (e) { return; }
      } else {
        this.comboForm = {
          name: '', categoryId: null, price: 0, description: '', image: '',
          comboItems: [],
        };
      }
      this.loadAllGoodsList();
      this.comboDialog = true;
    },

    addComboGoods() {
      this.comboForm.comboItems.push({ goodsId: null, name: '', price: 0, copies: 1 });
    },

    onPickGoods(row) {
      const d = this.allGoodsList.find((x) => x.id === row.goodsId);
      if (d) {
        row.name = d.name;
        row.price = d.price;
      }
    },

    async saveCombo() {
      if (!this.comboForm.name || !this.comboForm.categoryId) {
        ElementPlus.ElMessage.warning('名称与分类为必填项');
        return;
      }
      const goodsList = (this.comboForm.comboItems || []).filter((x) => x.goodsId);
      if (!goodsList.length) {
        ElementPlus.ElMessage.warning('请至少添加一项物资');
        return;
      }
      const payload = Object.assign({}, this.comboForm, { comboItems: goodsList });
      this.saving = true;
      try {
        if (payload.id) await api.put('/combo', payload);
        else await api.post('/combo', payload);
        ElementPlus.ElMessage.success('保存成功');
        this.comboDialog = false;
        this.loadCombos(this.comboPage);
      } catch (e) { /* ignore */ } finally { this.saving = false; }
    },

    async toggleCombo(row) {
      const next = row.status === 1 ? 0 : 1;
      try {
        await api.post('/combo/status/' + next, null, { id: row.id });
        ElementPlus.ElMessage.success('已更新状态');
        this.loadCombos(this.comboPage);
      } catch (e) { /* ignore */ }
    },

    async deleteCombo(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确定删除该组合包吗？', '提示', { type: 'warning' });
        await api.del('/combo', { ids: row.id });
        ElementPlus.ElMessage.success('已删除');
        this.loadCombos(this.comboPage);
      } catch (e) { /* ignore */ }
    },

    async deleteCombos() {
      if (!this.comboSelection.length) return;
      try {
        await ElementPlus.ElMessageBox.confirm(
          `确定删除选中的 ${this.comboSelection.length} 个组合包吗？`, '提示', { type: 'warning' });
        await api.del('/combo', { ids: this.comboSelection.map((s) => s.id).join(',') });
        ElementPlus.ElMessage.success('已删除');
        this.loadCombos(this.comboPage);
      } catch (e) { /* ignore */ }
    },

    // ---------------- 申领单 ----------------
    async loadOrderStat() {
      try { this.orderStat = (await api.get('/order/statistics')) || {}; } catch (e) { /* ignore */ }
    },

    async loadOrders(page) {
      this.orderPage = page || 1;
      const q = {
        page: this.orderPage, pageSize: 10,
        number: this.orderQuery.number, phone: this.orderQuery.phone,
        status: this.orderQuery.status,
      };
      if (this.orderDateRange && this.orderDateRange.length === 2) {
        q.beginTime = this.orderDateRange[0] + ' 00:00:00';
        q.endTime = this.orderDateRange[1] + ' 23:59:59';
      }
      try {
        const r = await api.get('/order/conditionSearch', q);
        this.orders = (r && r.records) || [];
        this.orderTotal = (r && r.total) || 0;
      } catch (e) { /* ignore */ }
    },

    orderStatusText(s) { return ORDER_STATUS_TEXT[s] || '未知'; },
    orderStatusTag(s) { return ORDER_STATUS_TAG[s] || 'info'; },

    async viewOrder(row) {
      try {
        this.currentOrder = await api.get('/order/details/' + row.id);
        this.orderDrawer = true;
      } catch (e) { /* ignore */ }
    },

    async approveOrder(row) {
      try {
        await ElementPlus.ElMessageBox.confirm(
          '确认通过该申领单？通过后进入采购流程。', '审批通过', { type: 'info' });
        await api.put('/order/confirm', { id: row.id });
        ElementPlus.ElMessage.success('已审批通过');
        this.loadOrders(this.orderPage);
        this.loadOrderStat();
      } catch (e) { /* ignore */ }
    },

    openReject(row) {
      this.rejectForm = { id: row.id, rejectionReason: '' };
      this.rejectDialog = true;
    },

    async doReject() {
      if (!this.rejectForm.rejectionReason) {
        ElementPlus.ElMessage.warning('请填写驳回理由');
        return;
      }
      try {
        await api.put('/order/rejection', this.rejectForm);
        ElementPlus.ElMessage.success('已驳回，部门预算已返还');
        this.rejectDialog = false;
        this.loadOrders(this.orderPage);
        this.loadOrderStat();
      } catch (e) { /* ignore */ }
    },

    openCancel(row) {
      this.cancelForm = { id: row.id, cancelReason: '' };
      this.cancelDialog = true;
    },

    async doCancel() {
      if (!this.cancelForm.cancelReason) {
        ElementPlus.ElMessage.warning('请填写取消原因');
        return;
      }
      try {
        await api.put('/order/cancel', this.cancelForm);
        ElementPlus.ElMessage.success('已取消，部门预算已返还');
        this.cancelDialog = false;
        this.loadOrders(this.orderPage);
        this.loadOrderStat();
      } catch (e) { /* ignore */ }
    },

    async deliverOrder(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确认发货？', '发货', { type: 'info' });
        await api.put('/order/delivery/' + row.id);
        ElementPlus.ElMessage.success('已发货');
        this.loadOrders(this.orderPage);
        this.loadOrderStat();
      } catch (e) { /* ignore */ }
    },

    async completeOrder(row) {
      try {
        await api.put('/order/complete/' + row.id);
        ElementPlus.ElMessage.success('已核销完成');
        this.loadOrders(this.orderPage);
        this.loadOrderStat();
      } catch (e) { /* ignore */ }
    },

    // ---------------- 员工 ----------------
    async loadEmployees(page) {
      this.empPage = page || 1;
      try {
        const r = await api.get('/employee/page', {
          page: this.empPage, pageSize: 10,
          name: this.empQuery.name, role: this.empQuery.role, deptId: this.empQuery.deptId,
        });
        this.employees = (r && r.records) || [];
        this.empTotal = (r && r.total) || 0;
      } catch (e) { /* ignore */ }
    },

    roleText(r) { return ROLE_TEXT[r] || '—'; },
    roleTag(r) { return ROLE_TAG[r] || 'info'; },

    openEmployee(row) {
      this.ensureDepartments();
      this.empForm = row
        ? {
          id: row.id, name: row.name, username: row.username, phone: row.phone,
          sex: row.sex, idNumber: row.idNumber, role: row.role, deptId: row.deptId,
        }
        : { name: '', username: '', phone: '', sex: '1', idNumber: '', role: 'EMPLOYEE', deptId: null };
      this.empDialog = true;
    },

    async saveEmployee() {
      if (!this.empForm.name || !this.empForm.username) {
        ElementPlus.ElMessage.warning('姓名与登录账号为必填项');
        return;
      }
      this.saving = true;
      try {
        if (this.empForm.id) await api.put('/employee', this.empForm);
        else await api.post('/employee', this.empForm);
        ElementPlus.ElMessage.success('保存成功，默认密码为 123456');
        this.empDialog = false;
        this.loadEmployees(this.empPage);
      } catch (e) { /* ignore */ } finally { this.saving = false; }
    },

    async toggleEmployee(row) {
      const next = row.status === 1 ? 0 : 1;
      try {
        await api.post('/employee/status/' + next, null, { id: row.id });
        ElementPlus.ElMessage.success('已更新状态');
        this.loadEmployees(this.empPage);
      } catch (e) { /* ignore */ }
    },

    // ---------------- 部门 ----------------
    async loadDepartments() {
      try {
        this.departmentList = (await api.get('/department/list', { name: this.deptQuery.name })) || [];
      } catch (e) { /* ignore */ }
    },

    openDepartment(row) {
      this.deptForm = row
        ? { id: row.id, name: row.name, code: row.code, leaderId: row.leaderId, sort: row.sort, status: row.status }
        : { name: '', code: '', leaderId: null, sort: 0, status: 1 };
      this.deptDialog = true;
    },

    async saveDepartment() {
      if (!this.deptForm.name || !this.deptForm.code) {
        ElementPlus.ElMessage.warning('部门名称与编码为必填项');
        return;
      }
      this.saving = true;
      try {
        if (this.deptForm.id) await api.put('/department', this.deptForm);
        else await api.post('/department', this.deptForm);
        ElementPlus.ElMessage.success('保存成功');
        this.deptDialog = false;
        this.loadDepartments();
        this.departments = (await api.get('/department/listEnabled')) || [];
      } catch (e) { /* ignore */ } finally { this.saving = false; }
    },

    async deleteDepartment(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确定删除该部门吗？部门下还有员工时不允许删除。', '提示',
          { type: 'warning' });
        await api.del('/department', { id: row.id });
        ElementPlus.ElMessage.success('已删除');
        this.loadDepartments();
      } catch (e) { /* ignore */ }
    },

    // ---------------- 预算 ----------------
    async loadBudgets() {
      try {
        this.budgetList = (await api.get('/budget/list', {
          deptId: this.budgetQuery.deptId, period: this.budgetQuery.period,
        })) || [];
      } catch (e) { /* ignore */ }
    },

    usedPercent(row) {
      if (!row.totalAmount || Number(row.totalAmount) === 0) return 0;
      return Math.min(Math.round((row.usedAmount / row.totalAmount) * 100), 100);
    },

    openBudget(row) {
      this.ensureDepartments();
      this.budgetForm = row
        ? { id: row.id, deptId: row.deptId, period: row.period, totalAmount: Number(row.totalAmount) }
        : { deptId: null, period: null, totalAmount: 0 };
      this.budgetDialog = true;
    },

    async saveBudget() {
      if (!this.budgetForm.deptId || !this.budgetForm.period) {
        ElementPlus.ElMessage.warning('部门与预算周期为必填项');
        return;
      }
      this.saving = true;
      try {
        if (this.budgetForm.id) await api.put('/budget', this.budgetForm);
        else await api.post('/budget', this.budgetForm);
        ElementPlus.ElMessage.success('保存成功');
        this.budgetDialog = false;
        this.loadBudgets();
      } catch (e) { /* ignore */ } finally { this.saving = false; }
    },

  },
}).use(ElementPlus, { locale: ElementPlusLocaleZhCn }).mount('#app');
