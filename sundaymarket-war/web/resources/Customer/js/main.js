(function ($) {
    "use strict";

    // Spinner
    var spinner = function () {
        setTimeout(function () {
            if ($('#spinner').length > 0) {
                $('#spinner').removeClass('show');
            }
        }, 1);
    };
    spinner(0);


    // Fixed Navbar
    $(window).scroll(function () {
        if ($(window).width() < 992) {
            if ($(this).scrollTop() > 55) {
                $('.fixed-top').addClass('shadow');
            } else {
                $('.fixed-top').removeClass('shadow');
            }
        } else {
            if ($(this).scrollTop() > 55) {
                $('.fixed-top').addClass('shadow').css('top', -55);
            } else {
                $('.fixed-top').removeClass('shadow').css('top', 0);
            }
        } 
    });
    
    
   // Back to top button
   $(window).scroll(function () {
    if ($(this).scrollTop() > 300) {
        $('.back-to-top').fadeIn('slow');
    } else {
        $('.back-to-top').fadeOut('slow');
    }
    });
    $('.back-to-top').click(function () {
        $('html, body').animate({scrollTop: 0}, 1500, 'easeInOutExpo');
        return false;
    });


    // Testimonial carousel
    $(".testimonial-carousel").owlCarousel({
        autoplay: true,
        smartSpeed: 2000,
        center: false,
        dots: true,
        loop: true,
        margin: 25,
        nav : true,
        navText : [
            '<i class="bi bi-arrow-left"></i>',
            '<i class="bi bi-arrow-right"></i>'
        ],
        responsiveClass: true,
        responsive: {
            0:{
                items:1
            },
            576:{
                items:1
            },
            768:{
                items:1
            },
            992:{
                items:2
            },
            1200:{
                items:2
            }
        }
    });


    // vegetable carousel
    $(".vegetable-carousel").owlCarousel({
        autoplay: true,
        smartSpeed: 1500,
        center: false,
        dots: true,
        loop: true,
        margin: 25,
        nav : true,
        navText : [
            '<i class="bi bi-arrow-left"></i>',
            '<i class="bi bi-arrow-right"></i>'
        ],
        responsiveClass: true,
        responsive: {
            0:{
                items:1
            },
            576:{
                items:1
            },
            768:{
                items:2
            },
            992:{
                items:3
            },
            1200:{
                items:4
            }
        }
    });


    // Modal Video
    $(document).ready(function () {
        var $videoSrc;
        $('.btn-play').click(function () {
            $videoSrc = $(this).data("src");
        });
        console.log($videoSrc);

        $('#videoModal').on('shown.bs.modal', function (e) {
            $("#video").attr('src', $videoSrc + "?autoplay=1&amp;modestbranding=1&amp;showinfo=0");
        })

        $('#videoModal').on('hide.bs.modal', function (e) {
            $("#video").attr('src', $videoSrc);
        })
    });



    // Product Quantity
    $('.quantity button').on('click', function () {
        var button = $(this);
        var oldValue = button.parent().parent().find('input').val();
        if (button.hasClass('btn-plus')) {
            var newVal = parseFloat(oldValue) + 1;
        } else {
            if (oldValue > 0) {
                var newVal = parseFloat(oldValue) - 1;
            } else {
                newVal = 0;
            }
        }
        button.parent().parent().find('input').val(newVal);
    });
    
    
document.addEventListener("DOMContentLoaded", function () {

    /* =====================================================
       SIDEBAR MENU: mở/đóng, xoay mũi tên, đóng mục khác
    ===================================================== */
    const toggles = document.querySelectorAll(".category-toggle");

    toggles.forEach(toggle => {
        toggle.addEventListener("click", function () {
            const target = document.querySelector(this.dataset.target);

            document.querySelectorAll(".category-sub").forEach(s => {
                if (s !== target) s.classList.remove("show");
            });

            toggles.forEach(tg => {
                if (tg !== this) tg.classList.remove("open");
            });

            this.classList.toggle("open");
            target.classList.toggle("show");
        });
    });

    /* =====================================================
       ACTIVE LINK TRONG SUBMENU
    ===================================================== */
    const subLinks = document.querySelectorAll(".sub-link");

    subLinks.forEach(link => {
        link.addEventListener("click", function () {
            subLinks.forEach(l => l.classList.remove("active"));
            this.classList.add("active");
        });
    });

    /* =====================================================
       TOP CATEGORY (Bách Hóa Xanh)
    ===================================================== */
    const topCats = document.querySelectorAll(".bhx-cat-item");

    topCats.forEach(item => {
        item.addEventListener("click", function () {
            topCats.forEach(i => i.classList.remove("active"));
            this.classList.add("active");
        });
    });

    /* =====================================================
       SUB CATEGORY BELOW (Bách Hóa Xanh)
    ===================================================== */
    const subCats = document.querySelectorAll(".bhx-sub-item");

    subCats.forEach(item => {
        item.addEventListener("click", function () {
            subCats.forEach(i => i.classList.remove("active"));
            this.classList.add("active");
        });
    });
   
});

document.addEventListener("DOMContentLoaded", function () {

    // Giả lập mã đơn (sau này backend đổ vô)
    const orderCode = "DH" + Math.floor(100000 + Math.random() * 900000);

    // Gán nội dung chuyển khoản
    document.getElementById("transferContent").innerText =
        "THANH TOAN DON " + orderCode;
});

// chọn phương thức
function selectPayment(type) {
    const qrBox = document.getElementById("qrBox");

    if (type === "BANK") {
        qrBox.style.display = "block";
    } else {
        qrBox.style.display = "none";
    }
}

// validate trước khi submit
function submitOrder() {
    const bankSelected = document.querySelector(
        'input[name="paymentMethod"]:not(:checked)'
    ) === null;

    const qrVisible = document.getElementById("qrBox").style.display === "block";
    const confirmed = document.getElementById("confirmTransferred").checked;

    if (qrVisible && !confirmed) {
        alert("⚠ Vui lòng xác nhận đã chuyển khoản trước khi đặt hàng!");
        return;
    }

    alert("✅ Đặt hàng thành công!\nCảm ơn bạn đã mua sắm.");
}

})(jQuery);

