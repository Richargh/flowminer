import { describe, it, expect } from 'vitest';

describe('Placeholder Test', () => {
  it('renders a simple element in the browser', () => {
    const div = document.createElement('div');
    div.textContent = 'Hello Vitest Browser';
    document.body.appendChild(div);

    expect(div.textContent).toBe('Hello Vitest Browser');
    expect(document.body.contains(div)).toBe(true);

    div.remove();
  });
});
