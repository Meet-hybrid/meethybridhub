"use client";

import { useState } from "react";
import { Mail, MapPin, Phone, Send, CheckCircle } from "lucide-react";
import { useStore } from "@/components/StoreProvider";

const offerCopyFor = (isDivinez: boolean) => isDivinez
  ? [
      ["Beaded bags", "Handmade clutches, purses, and carry pieces with a distinct point of view."],
      ["Jewellery", "Bracelets, anklets, necklaces, and chokers finished for everyday or occasion wear."],
      ["Waist beads", "Traditional and modern waist beads in considered colors, lengths, and combinations."],
      ["Sets & gifts", "Curated pairings for birthdays, weddings, anniversaries, and meaningful moments."],
      ["Custom pieces", "A personal color story, pattern, length, or design made around your brief."],
      ["Care & repair", "Practical guidance and support to help your favorite pieces stay beautiful."],
    ]
  : [
      ["Clothing", "Easy silhouettes and well-cut layers designed to earn their place in your wardrobe."],
      ["Shoes", "Clean-profile footwear selected for comfort, quality, and the way you actually move."],
      ["The edit", "Outfit-led collections that make getting dressed feel clear, personal, and considered."],
      ["New arrivals", "A steady rotation of pieces with a quiet point of view and lasting appeal."],
      ["Personal edit", "Tell us what you need and we will help you find the right size, shape, and pairing."],
      ["Aftercare", "Straightforward delivery, exchange, and support from Lagos to wherever you are."],
    ];

const valuesFor = (isDivinez: boolean) => isDivinez
  ? [["Made by hand", "Every piece receives patient attention, from the first bead to the final finish."], ["Personal by nature", "Color, texture, and proportion come together to make something that feels like yours."], ["For everyone", "Our accessories are designed for anyone who appreciates thoughtful beadwork."]]
  : [["Good proportion", "We look closely at fit, shape, and how a piece works with the rest of your wardrobe."], ["Useful beauty", "The best pieces are expressive without becoming difficult to live with."], ["A personal eye", "Every selection reflects a clear point of view, chosen with real people and real days in mind."]];

export default function AboutPage() {
  const activeStore = useStore();
  const isDivinez = activeStore.slug === "divinez-signature";
  const offerCopy = offerCopyFor(isDivinez);
  const values = valuesFor(isDivinez);
  const [contactSubmitted, setContactSubmitted] = useState(false);
  const [contactForm, setContactForm] = useState({ name: "", email: "", message: "" });
  const subject = isDivinez ? "bead accessories" : "clothing and footwear";

  if (isDivinez) {
    return (
      <div className="min-h-screen bg-[#FAFAF8] text-[#0B4A2B]">
        {}
        <section className="about-hero-divinez py-20">
          <div className="max-w-7xl mx-auto px-4 text-center">
            <p className="text-[#FAFAF8]/80 font-medium text-sm tracking-widest uppercase mb-2">{activeStore.subBrand}</p>
            <p className="text-[#FAFAF8]/60 text-xs tracking-widest uppercase mb-4">{activeStore.subBrandDescriptor}</p>
            <h1 className="text-4xl md:text-5xl font-serif font-bold text-[#FAFAF8] mb-4">The story behind the store</h1>
            <p className="text-[#FAFAF8]/80 text-lg max-w-2xl mx-auto">{activeStore.description}</p>
          </div>
        </section>

        {}
        <section className="py-16">
          <div className="max-w-7xl mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <p className="eyebrow">Our point of view</p>
              <h2 className="text-3xl font-serif font-bold text-[#0B4A2B] mb-6">{activeStore.tagline}</h2>
              <div className="space-y-4 text-[#0B4A2B]/70 leading-relaxed">
                <p>{activeStore.name} is an Abuja-based brand built around the pleasure of finding something that feels exactly right.</p>
                <p>We make and curate beaded accessories with warmth, patience, and a respect for the craft. Each piece is designed to carry a little personality.</p>
                <p>From a small everyday bracelet to a statement bag, our pieces are made to be worn, shared, and remembered.</p>
              </div>
            </div>
            <div className="about-stat-panel">
              <div className="grid grid-cols-2 gap-6">
                {[["500+", "Happy customers"], ["200+", "Unique designs"], ["50+", "Custom orders"], ["100%", "Thoughtfully selected"]].map(([number, label]) => (
                  <div key={label}>
                    <p className="text-3xl font-bold text-[#0B4A2B]">{number}</p>
                    <p className="text-sm text-[#0B4A2B]/60 mt-1">{label}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {}
        <section className="py-16 bg-white border-y border-[#0B4A2B]/10">
          <div className="max-w-7xl mx-auto px-4">
            <p className="eyebrow text-center">The experience</p>
            <h2 className="text-3xl font-serif font-bold text-[#0B4A2B] text-center mb-12">What we offer</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              {offerCopy.map(([title, description], index) => (
                <div key={title} className="about-card">
                  <span className="about-index">0{index + 1}</span>
                  <h3 className="text-xl font-bold text-[#0B4A2B] mb-3">{title}</h3>
                  <p className="text-[#0B4A2B]/60 text-sm leading-relaxed">{description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {}
        <section className="py-16">
          <div className="max-w-7xl mx-auto px-4">
            <p className="eyebrow text-center">What matters to us</p>
            <h2 className="text-3xl font-serif font-bold text-[#0B4A2B] text-center mb-12">Why {activeStore.name}</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              {values.map(([title, description]) => (
                <div key={title} className="about-card about-card-dark">
                  <h3 className="text-xl font-bold text-[#FAFAF8] mb-3">{title}</h3>
                  <p className="text-[#FAFAF8]/70 text-sm leading-relaxed">{description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {}
        <section id="contact" className="py-16 bg-white border-t border-[#0B4A2B]/10">
          <div className="max-w-7xl mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12">
            <div>
              <p className="eyebrow">Let&apos;s talk</p>
              <h2 className="text-3xl font-serif font-bold text-[#0B4A2B] mb-6">Get in touch</h2>
              <p className="text-[#0B4A2B]/70 mb-8">Have a question about our {subject}? Need help finding the right piece? We&apos;d love to hear from you.</p>
              <div className="space-y-4 text-sm">
                <p className="flex items-center gap-4"><Mail className="text-[#0B4A2B]" /> hello@{activeStore.slug}.com</p>
                <p className="flex items-center gap-4"><Phone className="text-[#0B4A2B]" /> <a href="https://wa.me/2348034704909" target="_blank" rel="noreferrer" className="hover:underline">WhatsApp support</a></p>
                <p className="flex items-center gap-4"><MapPin className="text-[#0B4A2B]" /> {activeStore.location}</p>
              </div>
              <div className="mt-6 flex flex-wrap gap-4 text-sm font-medium">
                <a href="https://www.instagram.com/beads_bydivine?igsh=M2l5ZzcybXVldnNn" target="_blank" rel="noreferrer" className="text-[#0B4A2B] hover:underline">Instagram</a>
                <a href="https://www.facebook.com/share/1DWr8Z45Q7/?mibextid=wwXIfr" target="_blank" rel="noreferrer" className="text-[#0B4A2B] hover:underline">Facebook</a>
              </div>
            </div>
            <div className="about-form">
              {contactSubmitted ? (
                <div className="text-center py-8">
                  <CheckCircle className="w-10 h-10 text-[#0B4A2B] mx-auto mb-4" />
                  <h3 className="font-semibold text-[#0B4A2B] mb-2">Message sent</h3>
                  <p className="text-sm text-[#0B4A2B]/60">We&apos;ll get back to you within 24 hours.</p>
                </div>
              ) : (
                <form onSubmit={(e) => { e.preventDefault(); setContactSubmitted(true); }} className="space-y-4">
                  <label>Name<input required value={contactForm.name} onChange={(e) => setContactForm({ ...contactForm, name: e.target.value })} placeholder="Your name" /></label>
                  <label>Email<input required type="email" value={contactForm.email} onChange={(e) => setContactForm({ ...contactForm, email: e.target.value })} placeholder="you@example.com" /></label>
                  <label>Message<textarea required rows={4} value={contactForm.message} onChange={(e) => setContactForm({ ...contactForm, message: e.target.value })} placeholder={`Tell us about your ${subject} needs...`} /></label>
                  <button type="submit"><Send className="w-4 h-4" /> Send message</button>
                </form>
              )}
            </div>
          </div>
        </section>
      </div>
    );
  }

  // MeethybridHub (default dark/amber theme)
  return (
    <div className="min-h-screen">
      <section className="about-hero text-white py-20">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <p className="text-amber-200 font-medium text-sm tracking-widest uppercase mb-4">{activeStore.name}</p>
          <h1 className="text-4xl md:text-5xl font-bold mb-4">The story behind the store</h1>
          <p className="text-amber-100 text-lg max-w-2xl mx-auto">{activeStore.description}</p>
        </div>
      </section>

      <section className="py-16 bg-[#17110d] text-[#f7f1e8]">
        <div className="max-w-7xl mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div>
            <p className="eyebrow">Our point of view</p>
            <h2 className="text-3xl font-bold text-white mb-6">{activeStore.tagline}</h2>
            <div className="space-y-4 text-[#c4b4a4] leading-relaxed">
              <p>{activeStore.name} is a Lagos-based brand built around the pleasure of finding something that feels exactly right.</p>
              <p>We select clothing and footwear for people who care about how things fit, feel, and live beyond the first wear. The result is a wardrobe with clarity and character.</p>
              <p>Our edit is intentionally focused: fewer, better decisions that help you dress with confidence on ordinary days and important ones.</p>
            </div>
          </div>
          <div className="about-stat-panel">
            <div className="grid grid-cols-2 gap-6">
              {[["500+", "Happy customers"], ["200+", "Curated pieces"], ["50+", "Personal edits"], ["100%", "Thoughtfully selected"]].map(([number, label]) => (
                <div key={label}>
                  <p className="text-3xl font-bold text-amber-600">{number}</p>
                  <p className="text-sm text-gray-500 mt-1">{label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="py-16 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4">
          <p className="eyebrow text-center">The experience</p>
          <h2 className="text-3xl font-bold text-gray-900 text-center mb-12">What we offer</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {offerCopy.map(([title, description], index) => (
              <div key={title} className="about-card">
                <span className="about-index">0{index + 1}</span>
                <h3 className="text-xl font-bold text-gray-900 mb-3">{title}</h3>
                <p className="text-gray-600 text-sm leading-relaxed">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-16 bg-[#17110d] text-[#f7f1e8]">
        <div className="max-w-7xl mx-auto px-4">
          <p className="eyebrow text-center">What matters to us</p>
          <h2 className="text-3xl font-bold text-white text-center mb-12">Why {activeStore.name}</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {values.map(([title, description]) => (
              <div key={title} className="about-card about-card-dark">
                <h3 className="text-xl font-bold text-white mb-3">{title}</h3>
                <p className="text-[#c4b4a4] text-sm leading-relaxed">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="contact" className="py-16 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12">
          <div>
            <p className="eyebrow">Let&apos;s talk</p>
            <h2 className="text-3xl font-bold text-gray-900 mb-6">Get in touch</h2>
            <p className="text-gray-600 mb-8">Have a question about our {subject}? Need help finding the right piece? We&apos;d love to hear from you.</p>
            <div className="space-y-4 text-sm">
              <p className="flex items-center gap-4"><Mail className="text-amber-600" /> hello@{activeStore.slug}.com</p>
              <p className="flex items-center gap-4"><Phone className="text-amber-600" /> WhatsApp support</p>
              <p className="flex items-center gap-4"><MapPin className="text-amber-600" /> {activeStore.location}</p>
            </div>
          </div>
          <div className="about-form">
            {contactSubmitted ? (
              <div className="text-center py-8">
                <CheckCircle className="w-10 h-10 text-green-600 mx-auto mb-4" />
                <h3 className="font-semibold text-gray-900 mb-2">Message sent</h3>
                <p className="text-sm text-gray-500">We&apos;ll get back to you within 24 hours.</p>
              </div>
            ) : (
              <form onSubmit={(e) => { e.preventDefault(); setContactSubmitted(true); }} className="space-y-4">
                <label>Name<input required value={contactForm.name} onChange={(e) => setContactForm({ ...contactForm, name: e.target.value })} placeholder="Your name" /></label>
                <label>Email<input required type="email" value={contactForm.email} onChange={(e) => setContactForm({ ...contactForm, email: e.target.value })} placeholder="you@example.com" /></label>
                <label>Message<textarea required rows={4} value={contactForm.message} onChange={(e) => setContactForm({ ...contactForm, message: e.target.value })} placeholder={`Tell us about your ${subject} needs...`} /></label>
                <button type="submit"><Send className="w-4 h-4" /> Send message</button>
              </form>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
