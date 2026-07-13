import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../../layout/navbar/navbar';

@Component({
  selector: 'app-server-error',
  standalone: true,
  imports: [RouterLink, Navbar],
  templateUrl: './server-error.html',
  styleUrl: './server-error.scss',
})
export class ServerError {}
