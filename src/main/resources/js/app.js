document.addEventListener('DOMContentLoaded', function() {
    const contentDiv = document.getElementById('content');

    function navigateTo(url) {
        history.pushState(null, null, url);
        router();
    }

    function router() {
        const route = window.location.pathname;
        if (route === '/admin') {
            contentDiv.innerHTML = '<h2>Admin Page</h2><p>Welcome to the admin page.</p>';
        } else {
            contentDiv.innerHTML = '<h2>Home Page</h2><p>Welcome to the home page.</p>';
        }
    }

    window.onpopstate = router;

    document.getElementById('homeLink').addEventListener('click', function(event) {
        event.preventDefault();
        navigateTo('/');
    });

    document.getElementById('adminLink').addEventListener('click', function(event) {
        event.preventDefault();
        navigateTo('/admin');
    });

    router();
});
