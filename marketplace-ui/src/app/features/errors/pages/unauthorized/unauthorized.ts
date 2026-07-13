import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../../layout/navbar/navbar';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [RouterLink, Navbar],
  templateUrl: './unauthorized.html',
  styleUrl: './unauthorized.scss',
})
export class Unauthorized {}
