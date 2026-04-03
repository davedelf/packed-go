import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent {
  constructor(private router: Router) {}

  goToCustomerLogin(): void {
    this.router.navigate(['/customer/login']);
  }

  goToAdminLogin(): void {
    this.router.navigate(['/admin/login']);
  }

  goToCustomerRegister(): void {
    this.router.navigate(['/customer/register']);
  }

  goToExploreEvents(): void {
    this.router.navigate(['/events']);
  }

  goToEvents(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  goToEmployeeLogin(): void {
    this.router.navigate(['/employee/login']);
  }

  goToTerms(): void {
    this.router.navigate(['/terms']);
  }

  goToPrivacy(): void {
    this.router.navigate(['/privacy']);
  }

  goToWinEvents(): void {
    this.router.navigate(['/admin/events']);
  }
  goToAnalytics(): void {
    this.router.navigate(['/admin/analytics']);
  }
  // FAQ toggle
  faqs = [
    {
      question: '¿Cómo compro una entrada?',
      answer: 'Regístrate en nuestra plataforma, busca el evento de tu interés, selecciona la cantidad de entradas y completa el pago. Recibirás tu entrada digital con código QR por email.',
      open: false
    },
    {
      question: '¿Puedo cancelar mi compra?',
      answer: 'Las cancelaciones dependen de la política del organizador del evento. Generalmente, si el evento es cancelado por el organizador, recibirás un reembolso completo.',
      open: false
    },
    {
      question: '¿Cómo funciona el código QR?',
      answer: 'Cada entrada tiene un código QR único que será escaneado en la entrada del evento. Puedes mostrarlo desde tu celular o imprimirlo. No compartas tu código QR ya que es de uso único.',
      open: false
    },
    {
      question: '¿Qué son las consumiciones?',
      answer: 'Algunas entradas incluyen consumiciones que puedes usar dentro del evento para comprar bebidas o comida. Cada vez que uses una consumición, el personal escaneará tu código QR.',
      open: false
    },
    {
      question: '¿Puedo transferir mi entrada a otra persona?',
      answer: 'Las entradas son personales e intransferibles salvo que el organizador del evento indique lo contrario.',
      open: false
    },
    {
      question: '¿Qué pasa si pierdo mi entrada digital?',
      answer: 'Puedes recuperar tu entrada accediendo a "Mis Órdenes" en tu cuenta.',
      open: false
    }
  ];

  toggleFaq(index: number): void {
    this.faqs[index].open = !this.faqs[index].open;
  }

  scrollToSection(sectionId: string): void {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
