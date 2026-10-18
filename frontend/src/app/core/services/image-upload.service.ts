import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ImageUploadService {
  private readonly maxFileSize = 5 * 1024 * 1024;
  private readonly maxDimension = 1400;

  async convert(file: File): Promise<string> {
    if (!file.type.startsWith('image/')) {
      throw new Error('Choisissez un fichier image.');
    }
    if (file.size > this.maxFileSize) {
      throw new Error('L’image doit peser moins de 5 Mo.');
    }

    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, this.maxDimension / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const context = canvas.getContext('2d');
    if (!context) {
      bitmap.close();
      throw new Error('Impossible de traiter cette image.');
    }

    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close();
    return canvas.toDataURL('image/webp', 0.82);
  }
}
