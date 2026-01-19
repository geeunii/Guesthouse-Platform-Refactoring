// Presentation Navigation Script - v1.7.0
class Presentation {
    constructor() {
        this.slides = document.querySelectorAll('.slide');
        this.currentSlide = 0;
        this.totalSlides = this.slides.length;
        this.init();
    }

    init() {
        this.createIndicators();
        this.bindEvents();
        this.updateIndicator();
        this.updateProgressBar();
        this.scalePresentation();
    }

    createIndicators() {
        const indicator = document.getElementById('indicator');
        if (!indicator) return;
        indicator.innerHTML = '';
        for (let i = 0; i < this.totalSlides; i++) {
            const dot = document.createElement('div');
            dot.className = 'indicator-dot' + (i === 0 ? ' active' : '');
            dot.addEventListener('click', () => this.goToSlide(i));
            indicator.appendChild(dot);
        }
    }

    bindEvents() {
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowRight' || e.key === ' ') {
                e.preventDefault();
                this.nextSlide();
            } else if (e.key === 'ArrowLeft') {
                e.preventDefault();
                this.prevSlide();
            }
        });

        document.addEventListener('click', (e) => {
            if (e.target.closest('.slide-indicator')) return;
            if (e.target.closest('button')) return;
            if (e.target.closest('a')) return;
            const x = e.clientX;
            const width = window.innerWidth;
            if (x > width * 0.6) this.nextSlide();
            else if (x < width * 0.4) this.prevSlide();
        });

        window.addEventListener('resize', () => this.scalePresentation());
    }

    goToSlide(index) {
        if (index < 0 || index >= this.totalSlides) return;
        this.slides[this.currentSlide].classList.remove('active');
        this.currentSlide = index;
        this.slides[this.currentSlide].classList.add('active');
        this.updateIndicator();
        this.updateProgressBar();
    }

    nextSlide() {
        if (this.currentSlide < this.totalSlides - 1) this.goToSlide(this.currentSlide + 1);
    }

    prevSlide() {
        if (this.currentSlide > 0) this.goToSlide(this.currentSlide - 1);
    }

    updateIndicator() {
        document.querySelectorAll('.indicator-dot').forEach((dot, i) => {
            dot.classList.toggle('active', i === this.currentSlide);
        });
    }

    updateProgressBar() {
        const progress = ((this.currentSlide + 1) / this.totalSlides) * 100;
        const bar = document.getElementById('progressBar');
        if (bar) bar.style.width = progress + '%';
    }

    scalePresentation() {
        const presentation = document.getElementById('presentation');
        const scaleX = window.innerWidth / 1920;
        const scaleY = window.innerHeight / 1080;
        presentation.style.transform = `scale(${Math.min(scaleX, scaleY)})`;
    }
}

document.addEventListener('DOMContentLoaded', () => new Presentation());
