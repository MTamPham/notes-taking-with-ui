package com.example.services;

import com.example.exception.ResourceNotFoundException;
import com.example.models.Note;
import com.example.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {
    @Autowired
    private NoteRepository noteRepository;

    public List<Note> getNotes() {
        return noteRepository.findAll();
    }

    public boolean existsById(Long noteId) {
        return noteRepository.existsById(noteId);
    }

    public boolean createNote(Note note) {
        Note savedNote = noteRepository.save(note);
        return savedNote != null;
    }

    public Note updateNote(Note note) {
        return noteRepository.findById(note.getId()).map(dbNote -> {
            dbNote.setContent(note.getContent());
            return noteRepository.save(dbNote);
        }).orElseThrow(() -> new ResourceNotFoundException("Couldn't find note with ID " + note.getId()));
    }

    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new ResourceNotFoundException("Couldn't find note with ID " + noteId);
        }
        noteRepository.deleteById(noteId);
    }
}
