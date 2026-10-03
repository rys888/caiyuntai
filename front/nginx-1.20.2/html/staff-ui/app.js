/**
 * 企业物资采购平台 · 员工采购端
 * 技术栈：Vue 3（CDN 全局构建）+ Element Plus，无需构建步骤，由 nginx 直接托管。
 *
 * 与后端约定：
 *  - 所有请求经 nginx 反向代理到 /user/**，前端统一加 /api 前缀
 *  - 员工端令牌请求头名为 authentication（管理端是 token），两端隔离
 *  - 后端统一返回 { code, msg, data }，code === 1 为成功
 */
const { createApp } = Vue;

const api = {
  tokenKey: 'cyt_staff_token',
  userKey: 'cyt_staff_user',

  getToken() {
    return localStorage.getItem(this.tokenKey);
  },
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
    if (token) headers['authentication'] = token;

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
        method,
        headers,
        body: data ? JSON.stringify(data) : undefined,
      });
    } catch (netErr) {
      // fetch 本身失败（后端/nginx 未启动、断网、请求被中断）时没有任何提示，
      // 而调用方普遍写成 catch(e){} 吞掉异常，最终表现为"点了按钮毫无反应"。
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
  post(url, data) { return this.request('POST', url, data); },
  put(url, data) { return this.request('PUT', url, data); },
  del(url, params) { return this.request('DELETE', url, null, params); },
};

const STATUS_TEXT = {
  1: '待提交',
  2: '待审批',
  3: '采购中',
  4: '配送中',
  5: '已完成',
  6: '已取消',
};
const STATUS_TAG = {
  1: 'info',
  2: 'warning',
  3: 'primary',
  4: 'primary',
  5: 'success',
  6: 'info',
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
      menus: [
        { key: 'home', label: '首页', icon: '🏠' },
        { key: 'goods', label: '物资目录', icon: '📦' },
        { key: 'packages', label: '组合申领包', icon: '🎁' },
        { key: 'cart', label: '申领车', icon: '🛒' },
        { key: 'orders', label: '我的申领单', icon: '📋' },
        { key: 'address', label: '收货地址', icon: '📍' },
        { key: 'budget', label: '部门预算', icon: '💰' },
      ],

      // 首页
      stat: { waiting: 0, purchasing: 0, delivering: 0, finished: 0 },
      hotGoods: [],

      // 物资
      categories: [],
      activeCategory: null,
      goodsKeyword: '',
      goods: [],

      // 组合包
      packages: [],
      pkgDialog: false,
      // 组合包本身的信息（名称/价格），与包内物资列表分开存
      pkgInfo: null,
      // /combo/goods/{id} 返回的是物资数组，不是组合包对象
      pkgGoods: [],

      // 申领车
      cart: [],
      addresses: [],
      submitForm: { addressBookId: null, remark: '' },
      submitting: false,

      // 申领单
      orders: [],
      orderTotal: 0,
      orderPage: 1,
      orderStatus: null,
      orderDrawer: false,
      currentOrder: null,

      // 地址
      addrDialog: false,
      addrSaving: false,
      addrForm: {},

      // 预算
      budget: null,

      returnDialog: false,
      returnForm: { id: null, reason: '' },
    };
  },

  computed: {
    currentMenuLabel() {
      const m = this.menus.find((x) => x.key === this.page);
      return m ? m.label : '';
    },
    cartCount() {
      return this.cart.reduce((sum, item) => sum + item.number, 0);
    },
    totalAmount() {
      return this.cart
        .reduce((sum, item) => sum + item.amount * item.number, 0)
        .toFixed(2);
    },
    usedPercent() {
      if (!this.budget || Number(this.budget.totalAmount) === 0) return 0;
      const p = (this.budget.usedAmount / this.budget.totalAmount) * 100;
      return Math.min(Math.round(p), 100);
    },
  },

  mounted() {
    const cached = localStorage.getItem(api.userKey);
    if (cached && api.getToken()) {
      try {
        this.user = JSON.parse(cached);
        this.loadHome();
        this.loadBudget();
        this.loadCategories();
        this.loadCart();
        this.loadAddresses();
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
        const user = await api.post('/user/login', this.loginForm);
        api.setAuth(user.token, user);
        this.user = user;
        ElementPlus.ElMessage.success('登录成功');
        this.loadHome();
        this.loadBudget();
        this.loadCategories();
        this.loadGoods();
        this.loadCart();
        this.loadAddresses();
      } catch (e) {
        /* 错误提示已在 request 中统一处理 */
      } finally {
        this.loginLoading = false;
      }
    },

    async doLogout() {
      try { await api.post('/user/logout'); } catch (e) { /* 后端登出失败不阻塞本地清理 */ }
      api.clearAuth();
      this.user = null;
    },

    // ---------------- 导航 ----------------
    go(key) {
      this.page = key;
      if (key === 'home') this.loadHome();
      if (key === 'goods') { this.loadCategories(); this.loadGoods(); }
      if (key === 'packages') this.loadPackages();
      if (key === 'cart') { this.loadCart(); this.loadAddresses(); }
      if (key === 'orders') this.loadOrders(1);
      if (key === 'address') this.loadAddresses();
      if (key === 'budget') this.loadBudget();
    },

    // ---------------- 首页 ----------------
    async loadHome() {
      try {
        // 复用申领单查询接口做前端统计，避免为首页单独新增接口
        const [w, p, d, f, hot] = await Promise.all([
          api.get('/order/historyOrders', { page: 1, pageSize: 1, status: 2 }),
          api.get('/order/historyOrders', { page: 1, pageSize: 1, status: 3 }),
          api.get('/order/historyOrders', { page: 1, pageSize: 1, status: 4 }),
          api.get('/order/historyOrders', { page: 1, pageSize: 1, status: 5 }),
          api.get('/goods/list', { categoryId: this.activeCategory }),
        ]);
        this.stat = {
          waiting: w.total || 0,
          purchasing: p.total || 0,
          delivering: d.total || 0,
          finished: f.total || 0,
        };
        this.hotGoods = (hot || []).slice(0, 4);
      } catch (e) { /* ignore */ }
    },

    // ---------------- 物资 ----------------
    async loadCategories() {
      try {
        this.categories = (await api.get('/category/list', { type: 1 })) || [];
      } catch (e) { /* ignore */ }
    },

    async loadGoods() {
      try {
        this.goods = (await api.get('/goods/list', { categoryId: this.activeCategory })) || [];
        // 分类已在后端过滤，这里的搜索是前端二次筛选，避免为关键字搜索新增接口
        if (this.goodsKeyword) {
          const kw = this.goodsKeyword.toLowerCase();
          this.goods = this.goods.filter((g) => (g.name || '').toLowerCase().includes(kw));
        }
      } catch (e) { /* ignore */ }
    },

    selectCategory(id) {
      this.activeCategory = id;
      this.loadGoods();
    },

    // ---------------- 组合包 ----------------
    async loadPackages() {
      try {
        this.packages = (await api.get('/combo/list', { categoryId: this.activeCategory })) || [];
      } catch (e) { /* ignore */ }
    },

    async viewPackage(p) {
      try {
        // 接口 /combo/goods/{id} 返回的是「包内物资数组」（name/copies/image/description），
        // 组合包本身的名称与价格来自列表行 p，两者要分开存，否则弹窗会显示为空。
        this.pkgGoods = (await api.get('/combo/goods/' + p.id)) || [];
        this.pkgInfo = p;
        this.pkgDialog = true;
      } catch (e) { /* ignore */ }
    },

    // ---------------- 申领车 ----------------
    async loadCart() {
      try {
        this.cart = (await api.get('/shoppingCart/list')) || [];
      } catch (e) { /* ignore */ }
    },

    async addCart(goods, pkg) {
      const body = {};
      if (goods) {
        body.goodsId = goods.id;
        // 若物资带规格（goodsSpec），这里取第一个规格，用户可在明细中调整
        if (goods.flavors && goods.flavors.length) {
          body.goodsSpec = goods.flavors[0].value;
        }
      } else if (pkg) {
        body.comboId = pkg.id;
      } else {
        return;
      }
      try {
        await api.post('/shoppingCart/add', body);
        ElementPlus.ElMessage.success('已加入申领车');
        this.loadCart();
      } catch (e) { /* ignore */ }
    },

    // 申领车里点 "+"：需要带上原来的规格，否则会被当成新行插入
    addCartByRow(row) {
      const body = { number: 1 };
      if (row.goodsId) body.goodsId = row.goodsId;
      if (row.comboId) body.comboId = row.comboId;
      if (row.goodsSpec) body.goodsSpec = row.goodsSpec;
      api.post('/shoppingCart/add', body)
        .then(() => this.loadCart())
        .catch(() => {});
    },

    subCart(row) {
      const body = {};
      if (row.goodsId) body.goodsId = row.goodsId;
      if (row.comboId) body.comboId = row.comboId;
      if (row.goodsSpec) body.goodsSpec = row.goodsSpec;
      api.post('/shoppingCart/sub', body)
        .then(() => this.loadCart())
        .catch(() => {});
    },

    async cleanCart() {
      try {
        await ElementPlus.ElMessageBox.confirm('确定清空申领车吗？', '提示', { type: 'warning' });
        await api.del('/shoppingCart/clean');
        this.loadCart();
      } catch (e) { /* 取消或失败 */ }
    },

    async submitOrder() {
      if (!this.cart.length) {
        ElementPlus.ElMessage.warning('申领车是空的，请先添加物资');
        return;
      }
      // 刷新页面后 submitForm 会重置，这里先补取一次地址，避免"下拉框空着 → 点了没反应"
      if (!this.submitForm.addressBookId) await this.loadAddresses();
      if (!this.submitForm.addressBookId) {
        try {
          await ElementPlus.ElMessageBox.confirm('当前账号还没有收货地址，先去添加一个？', '无法提交申领单',
            { type: 'warning', confirmButtonText: '去添加', cancelButtonText: '取消' });
          this.go('address');
          this.openAddress();
        } catch (e) { /* 用户取消 */ }
        return;
      }
      this.submitting = true;
      try {
        await api.post('/order/submit', {
          addressBookId: this.submitForm.addressBookId,
          remark: this.submitForm.remark,
          amount: Number(this.totalAmount),
          payMethod: 1,
          packAmount: 0,
          tablewareNumber: 0,
          tablewareStatus: 0,
          deliveryStatus: 1,
        });
        ElementPlus.ElMessage.success('申领单已提交，等待审批');
        this.submitForm.remark = '';
        this.loadCart();
        this.loadBudget();
        this.go('orders');
      } catch (e) {
        // 业务错误（code!=1）与网络异常都已由 request 统一弹出提示，这里不再静默吞掉其它信息
      } finally {
        this.submitting = false;
      }
    },

    // ---------------- 申领单 ----------------
    async loadOrders(page) {
      this.orderPage = page || 1;
      try {
        const res = await api.get('/order/historyOrders', {
          page: this.orderPage,
          pageSize: 10,
          status: this.orderStatus,
        });
        this.orders = (res && res.records) || [];
        this.orderTotal = (res && res.total) || 0;
      } catch (e) { /* ignore */ }
    },

    orderGoodsText(row) {
      if (row.orderGoods) return row.orderGoods;
      const list = row.orderDetailList || [];
      if (!list.length) return '—';
      return list.map((d) => d.name + '×' + d.number).join('；');
    },

    statusText(s) { return STATUS_TEXT[s] || '未知'; },
    statusTagType(s) { return STATUS_TAG[s] || 'info'; },

    async viewOrder(row) {
      try {
        this.currentOrder = await api.get('/order/orderDetail/' + row.id);
        this.orderDrawer = true;
      } catch (e) { /* ignore */ }
    },

    async remind(row) {
      try {
        await api.get('/order/reminder/' + row.id);
        ElementPlus.ElMessage.success('已催办，采购专员会尽快处理');
      } catch (e) { /* ignore */ }
    },

    async cancelOrder(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确定撤销这张申领单吗？预算会返还。', '提示', { type: 'warning' });
        await api.put('/order/cancel/' + row.id);
        ElementPlus.ElMessage.success('已撤销');
        this.loadOrders(this.orderPage);
        this.loadBudget();
      } catch (e) { /* ignore */ }
    },

    async confirmReceipt(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确认已经收到物资？', '提示', { type: 'info' });
        await api.put('/order/confirmReceipt/' + row.id);
        ElementPlus.ElMessage.success('已确认收货');
        this.loadOrders(this.orderPage);
      } catch (e) { /* ignore */ }
    },

    async repetition(row) {
      try {
        await api.post('/order/repetition/' + row.id);
        ElementPlus.ElMessage.success('已重新加入申领车');
        this.loadCart();
        this.go('cart');
      } catch (e) { /* ignore */ }
    },

    openReturn(row) {
      this.returnForm = { id: row.id, reason: '' };
      this.returnDialog = true;
    },

    async doReturn() {
      if (!this.returnForm.reason) {
        ElementPlus.ElMessage.warning('请填写退换原因');
        return;
      }
      try {
        await api.post('/order/return/' + this.returnForm.id + '?reason=' +
          encodeURIComponent(this.returnForm.reason));
        ElementPlus.ElMessage.success('已提交退换申请');
        this.returnDialog = false;
        this.loadOrders(this.orderPage);
      } catch (e) { /* ignore */ }
    },

    // ---------------- 地址 ----------------
    async loadAddresses() {
      try {
        this.addresses = (await api.get('/addressBook/list')) || [];
        // 没有默认地址时退回第一条，避免下拉框空着、提交时又没有任何引导
        if (!this.submitForm.addressBookId && this.addresses.length) {
          const def = this.addresses.find((a) => a.isDefault === 1) || this.addresses[0];
          this.submitForm.addressBookId = def.id;
        }
      } catch (e) { /* 错误提示已在 request 中统一处理 */ }
    },

    aLabel(a) {
      return [a.deptName, a.station, a.detail].filter(Boolean).join(' / ');
    },

    openAddress(row) {
      this.addrForm = row
        ? Object.assign({}, row)
        : { consignee: '', deptName: '', station: '', detail: '', phone: '', isDefault: 0 };
      this.addrDialog = true;
    },

    async saveAddress() {
      if (!this.addrForm.consignee || !this.addrForm.detail || !this.addrForm.phone) {
        ElementPlus.ElMessage.warning('申领人、详细位置、联系电话为必填项');
        return;
      }
      this.addrSaving = true;
      try {
        if (this.addrForm.id) {
          await api.put('/addressBook', this.addrForm);
        } else {
          await api.post('/addressBook', this.addrForm);
        }
        ElementPlus.ElMessage.success('保存成功');
        this.addrDialog = false;
        this.loadAddresses();
      } catch (e) { /* ignore */ } finally {
        this.addrSaving = false;
      }
    },

    async setDefault(row) {
      try {
        await api.put('/addressBook/default', { id: row.id });
        ElementPlus.ElMessage.success('已设为默认');
        this.loadAddresses();
      } catch (e) { /* ignore */ }
    },

    async deleteAddress(row) {
      try {
        await ElementPlus.ElMessageBox.confirm('确定删除该地址吗？', '提示', { type: 'warning' });
        await api.del('/addressBook', { id: row.id });
        ElementPlus.ElMessage.success('已删除');
        this.loadAddresses();
      } catch (e) { /* ignore */ }
    },

    // ---------------- 预算 ----------------
    async loadBudget() {
      try {
        this.budget = await api.get('/budget/my');
      } catch (e) { /* ignore */ }
    },
  },
}).use(ElementPlus, { locale: ElementPlusLocaleZhCn }).mount('#app');
