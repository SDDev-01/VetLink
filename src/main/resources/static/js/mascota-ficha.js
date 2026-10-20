document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.vl-drawer-tab').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.vl-drawer-tab').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.vl-drawer-pane').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            var pane = document.querySelector('.vl-drawer-pane[data-pane="' + btn.dataset.tab + '"]');
            if (pane) pane.classList.add('active');
        });
    });
});
